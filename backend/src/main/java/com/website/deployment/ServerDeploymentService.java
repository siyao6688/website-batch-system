package com.website.deployment;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;

@Slf4j
@Service
public class ServerDeploymentService {

    @Value("${website.deployment.server.host}")
    private String serverHost;

    @Value("${website.deployment.server.port:22}")
    private int serverPort;

    @Value("${website.deployment.server.username}")
    private String serverUsername;

    @Value("${website.deployment.server.private-key-path}")
    private String privateKeyPath;

    @Value("${website.deployment.remote.web-root}")
    private String remoteWebRoot;

    @Value("${website.deployment.remote.nginx-sites-available}")
    private String nginxSitesAvailable;

    @Value("${website.deployment.remote.nginx-sites-enabled}")
    private String nginxSitesEnabled;

    @Value("${website.deployment.remote.nginx-reload-command}")
    private String nginxReloadCommand;

    @Value("${website.deployment.enabled:true}")
    private boolean deploymentEnabled;

    /**
     * 部署网站到远程服务器
     */
    public void deployWebsite(String domain, Path localWebsitePath) throws Exception {
        if (!deploymentEnabled) {
            log.info("部署功能已禁用，跳过部署 domain: {}", domain);
            return;
        }

        log.info("开始部署网站 domain: {} 到服务器 {}", domain, serverHost);

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        ChannelExec execChannel = null;

        try {
            // 1. 加载私钥并建立SSH连接
            jsch.addIdentity(privateKeyPath);
            session = jsch.getSession(serverUsername, serverHost, serverPort);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            log.info("SSH连接成功");

            // 2. 上传网站文件
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(30000);
            log.info("SFTP连接成功");

            // 创建远程目录
            String remoteDomainPath = remoteWebRoot + "/" + domain;
            createRemoteDirectory(sftpChannel, remoteDomainPath);

            // 上传文件
            uploadDirectory(sftpChannel, localWebsitePath, remoteDomainPath);
            log.info("网站文件上传完成: {}", remoteDomainPath);

            // 3. 配置Nginx虚拟主机
            createNginxConfig(domain, session);
            log.info("Nginx配置创建完成");

            // 4. 重载Nginx服务
            reloadNginx(session);
            log.info("Nginx重载完成");

            log.info("网站部署成功 domain: {}", domain);

        } catch (Exception e) {
            log.error("部署失败 domain: {}", domain, e);
            throw new RuntimeException("网站部署失败: " + e.getMessage(), e);
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (execChannel != null && execChannel.isConnected()) {
                execChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 创建远程目录
     */
    private void createRemoteDirectory(ChannelSftp sftpChannel, String remotePath) throws SftpException {
        try {
            sftpChannel.stat(remotePath);
            log.debug("远程目录已存在: {}", remotePath);
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                // 目录不存在，创建
                String[] folders = remotePath.split("/");
                String path = "";
                for (String folder : folders) {
                    if (!folder.isEmpty()) {
                        path += "/" + folder;
                        try {
                            sftpChannel.stat(path);
                        } catch (SftpException ex) {
                            if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                                sftpChannel.mkdir(path);
                                log.debug("创建目录: {}", path);
                            }
                        }
                    }
                }
            } else {
                throw e;
            }
        }
    }

    /**
     * 上传整个目录
     */
    private void uploadDirectory(ChannelSftp sftpChannel, Path localPath, String remotePath) throws IOException, SftpException {
        File localDir = localPath.toFile();
        if (!localDir.exists() || !localDir.isDirectory()) {
            throw new IOException("本地目录不存在: " + localPath);
        }

        File[] files = localDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String remoteFilePath = remotePath + "/" + file.getName();

            if (file.isDirectory()) {
                // 递归上传子目录
                createRemoteDirectory(sftpChannel, remoteFilePath);
                uploadDirectory(sftpChannel, file.toPath(), remoteFilePath);
            } else {
                // 上传文件
                try (InputStream inputStream = new FileInputStream(file)) {
                    sftpChannel.put(inputStream, remoteFilePath);
                    log.debug("上传文件: {} -> {}", file.getPath(), remoteFilePath);
                }
            }
        }
    }

    /**
     * 创建Nginx虚拟主机配置
     */
    private void createNginxConfig(String domain, Session session) throws Exception {
        String configContent = buildNginxConfig(domain);
        String tempConfigPath = "/tmp/" + domain + ".conf";
        String finalConfigPath = nginxSitesAvailable + "/" + domain + ".conf";
        String enabledConfigPath = nginxSitesEnabled + "/" + domain + ".conf";

        // 1. 上传临时配置文件
        uploadStringToFile(session, configContent, tempConfigPath);

        // 2. 复制到sites-available
        String copyCmd = String.format("sudo cp %s %s", tempConfigPath, finalConfigPath);
        executeCommand(session, copyCmd);

        // 3. 创建符号链接到sites-enabled（如果不存在）
        String checkLinkCmd = String.format("test -L %s || sudo ln -sf %s %s",
            enabledConfigPath, finalConfigPath, enabledConfigPath);
        executeCommand(session, checkLinkCmd);

        // 4. 删除临时文件
        String deleteTempCmd = String.format("rm -f %s", tempConfigPath);
        executeCommand(session, deleteTempCmd);

        log.info("Nginx配置创建完成: {}", finalConfigPath);
    }

    /**
     * 构建Nginx配置
     */
    private String buildNginxConfig(String domain) {
        return String.format(
            "server {\n" +
            "    listen 80;\n" +
            "    server_name %s;\n" +
            "\n" +
            "    root %s/%s;\n" +
            "    index index.html index.htm;\n" +
            "\n" +
            "    location / {\n" +
            "        try_files $uri $uri/ /index.html;\n" +
            "    }\n" +
            "\n" +
            "    access_log /var/log/nginx/%s-access.log;\n" +
            "    error_log /var/log/nginx/%s-error.log;\n" +
            "}\n",
            domain, remoteWebRoot, domain, domain, domain
        );
    }

    /**
     * 上传字符串内容到远程文件
     */
    private void uploadStringToFile(Session session, String content, String remoteFilePath) throws Exception {
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(30000);

            try (InputStream inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"))) {
                sftpChannel.put(inputStream, remoteFilePath);
                log.debug("上传配置文件到: {}", remoteFilePath);
            }
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
        }
    }

    /**
     * 执行远程命令
     */
    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            try (InputStream in = channel.getInputStream();
                 InputStream err = channel.getErrStream()) {

                channel.connect(30000);

                // 读取输出
                StringBuilder output = new StringBuilder();
                byte[] buffer = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int bytesRead = in.read(buffer, 0, buffer.length);
                        if (bytesRead < 0) break;
                        output.append(new String(buffer, 0, bytesRead));
                    }
                    while (err.available() > 0) {
                        int bytesRead = err.read(buffer, 0, buffer.length);
                        if (bytesRead < 0) break;
                        output.append(new String(buffer, 0, bytesRead));
                    }
                    if (channel.isClosed()) {
                        if (in.available() > 0) continue;
                        if (err.available() > 0) continue;
                        break;
                    }
                    Thread.sleep(100);
                }

                int exitStatus = channel.getExitStatus();
                if (exitStatus != 0) {
                    throw new Exception("命令执行失败: " + command + "\n" + output);
                }

                return output.toString();
            }
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    /**
     * 重载Nginx服务
     */
    private void reloadNginx(Session session) throws Exception {
        String result = executeCommand(session, nginxReloadCommand);
        log.info("Nginx重载结果: {}", result);
    }

    /**
     * 测试服务器连接
     */
    public boolean testConnection() {
        if (!deploymentEnabled) {
            log.info("部署功能已禁用");
            return false;
        }

        JSch jsch = new JSch();
        Session session = null;

        try {
            jsch.addIdentity(privateKeyPath);
            session = jsch.getSession(serverUsername, serverHost, serverPort);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            log.info("服务器连接测试成功: {}", serverHost);
            return true;
        } catch (Exception e) {
            log.error("服务器连接测试失败: {}", serverHost, e);
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}