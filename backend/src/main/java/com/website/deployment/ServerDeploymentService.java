package com.website.deployment;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 网站状态检测结果
 */
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

    @Value("${website.deployment.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${website.deployment.retry.delay-ms:2000}")
    private int retryDelayMs;

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
        log.info("上传目录: {}, 文件数: {}", localDir.getAbsolutePath(), files != null ? files.length : 0);
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
                log.info("上传文件: {}, 大小: {} bytes", file.getAbsolutePath(), file.length());
                try (InputStream inputStream = new FileInputStream(file)) {
                    sftpChannel.put(inputStream, remoteFilePath);
                    log.info("上传完成: {} -> {}", file.getPath(), remoteFilePath);
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

        // 4. 测试Nginx配置（带ulimit设置解决"Too many open files"问题）
        String testConfigCmd = "sudo bash -c 'ulimit -n 65535 && nginx -t'";
        executeCommand(session, testConfigCmd);
        log.info("Nginx配置语法测试通过");

        // 5. 删除临时文件
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
            "    server_name %s www.%s;\n" +
            "\n" +
            "    root %s/%s;\n" +
            "    index index.html index.htm;\n" +
            "\n" +
            "    # 静态资源缓存\n" +
            "    location ~* \\.(css|js|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {\n" +
            "        expires 1y;\n" +
            "        add_header Cache-Control \"public, immutable\";\n" +
            "        try_files $uri $uri/ =404;\n" +
            "    }\n" +
            "\n" +
            "    location / {\n" +
            "        try_files $uri $uri/ /index.html;\n" +
            "    }\n" +
            "\n" +
            "    # 错误页面\n" +
            "    error_page 404 /index.html;\n" +
            "    error_page 500 502 503 504 /index.html;\n" +
            "\n" +
            "    access_log /var/log/nginx/%s-access.log;\n" +
            "    error_log /var/log/nginx/%s-error.log;\n" +
            "}\n",
            domain, domain, remoteWebRoot, domain, domain, domain
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
     * 执行远程命令（带重试机制）
     */
    private String executeCommand(Session session, String command) throws Exception {
        return executeCommand(session, command, false);
    }

    /**
     * 执行远程命令（带重试机制）
     * @param session SSH会话
     * @param command 命令
     * @param skipRetry 是否跳过重试（用于重试调用时避免无限循环）
     */
    private String executeCommand(Session session, String command, boolean skipRetry) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
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
                        String outputStr = output.toString();
                        // 检查是否为可重试的错误（如"Too many open files"）
                        if (!skipRetry && shouldRetry(outputStr) && attempt < retryMaxAttempts) {
                            log.warn("命令执行失败（尝试 {}/{}），将在 {}ms 后重试: {}",
                                attempt, retryMaxAttempts, retryDelayMs, outputStr);
                            Thread.sleep(retryDelayMs);
                            continue;
                        }
                        throw new Exception("命令执行失败: " + command + "\n" + outputStr);
                    }

                    return output.toString();
                }
            } catch (Exception e) {
                lastException = e;
                // 检查是否为可重试的错误
                if (!skipRetry && shouldRetry(e.getMessage()) && attempt < retryMaxAttempts) {
                    log.warn("命令执行异常（尝试 {}/{}），将在 {}ms 后重试: {}",
                        attempt, retryMaxAttempts, retryDelayMs, e.getMessage());
                    Thread.sleep(retryDelayMs);
                    continue;
                }
                throw e;
            } finally {
                if (channel != null && channel.isConnected()) {
                    channel.disconnect();
                }
            }
        }

        throw lastException;
    }

    /**
     * 判断错误是否应该重试
     */
    private boolean shouldRetry(String errorMessage) {
        if (errorMessage == null) return false;
        String lowerMessage = errorMessage.toLowerCase();
        return lowerMessage.contains("too many open files") ||
               lowerMessage.contains("resource temporarily unavailable") ||
               lowerMessage.contains("connection refused") ||
               lowerMessage.contains("connection reset") ||
               lowerMessage.contains("timeout");
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

    /**
     * 生成本地Nginx配置文件
     */
    public void generateLocalNginxConfig(String domain, Path localWebsitePath) {
        try {
            String configContent = buildNginxConfig(domain);
            // 保存到本地文件
            Path configFile = localWebsitePath.resolveSibling(domain + ".nginx.conf");
            Files.write(configFile, configContent.getBytes(StandardCharsets.UTF_8));
            log.info("Nginx配置文件已生成: {}", configFile.toAbsolutePath());
            log.info("请将配置文件复制到 /etc/nginx/sites-available/ 并启用配置");
            log.info("sudo cp {} /etc/nginx/sites-available/", configFile);
            log.info("sudo ln -sf /etc/nginx/sites-available/{}.conf /etc/nginx/sites-enabled/", domain);
            log.info("sudo nginx -t && sudo systemctl reload nginx");
        } catch (Exception e) {
            log.error("生成Nginx配置文件失败", e);
        }
    }

    /**
     * 检测网站部署状态
     * @param domain 域名
     * @return 状态结果
     */
    public WebsiteStatusResult checkWebsiteStatus(String domain) {
        if (!deploymentEnabled) {
            return new WebsiteStatusResult("disabled", "部署功能未启用");
        }

        JSch jsch = new JSch();
        Session session = null;

        try {
            jsch.addIdentity(privateKeyPath);
            session = jsch.getSession(serverUsername, serverHost, serverPort);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            // 检查网站文件
            boolean filesExist = checkRemoteFiles(session, domain);

            // 检查nginx配置
            boolean nginxConfigExist = checkNginxConfig(session, domain);

            // 构建状态结果
            String status;
            String description;
            if (filesExist && nginxConfigExist) {
                status = "normal";
                description = "网站部署正常";
            } else if (!filesExist && !nginxConfigExist) {
                status = "both_missing";
                description = "网站文件和nginx配置都不存在";
            } else if (!filesExist) {
                status = "files_missing";
                description = "网站文件不存在，nginx配置存在";
            } else {
                status = "nginx_missing";
                description = "网站文件存在，nginx配置不存在";
            }

            log.info("网站状态检测完成: {} -> {}", domain, status);
            return new WebsiteStatusResult(status, description);

        } catch (Exception e) {
            log.error("检测网站状态失败: {}", domain, e);
            return new WebsiteStatusResult("check_failed", "检测失败: " + e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 检查远程网站文件是否存在
     */
    private boolean checkRemoteFiles(Session session, String domain) throws Exception {
        String remotePath = remoteWebRoot + "/" + domain;
        String checkCmd = String.format("test -d %s && test -f %s/index.html", remotePath, remotePath);
        try {
            executeCommand(session, checkCmd);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查nginx配置是否存在
     */
    private boolean checkNginxConfig(Session session, String domain) throws Exception {
        String configPath = nginxSitesEnabled + "/" + domain + ".conf";
        String checkCmd = String.format("test -L %s || test -f %s", configPath, configPath);
        try {
            executeCommand(session, checkCmd);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 网站状态检测结果DTO
     */
    public static class WebsiteStatusResult {
        private final String status;
        private final String description;

        public WebsiteStatusResult(String status, String description) {
            this.status = status;
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public String getDescription() {
            return description;
        }
    }
}