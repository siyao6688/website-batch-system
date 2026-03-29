package com.website.generator;

import com.website.entity.Company;
import com.website.entity.WebsiteContent;
import com.website.entity.WebsiteTemplate;
import com.website.deployment.ServerDeploymentService;
import com.website.repository.WebsiteContentRepository;
import com.website.repository.WebsiteTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebsiteGeneratorService {

    private final WebsiteContentRepository contentRepository;
    private final WebsiteTemplateRepository templateRepository;
    private final ServerDeploymentService deploymentService;

    @Value("${website.output-path:/var/www/html}")
    private String outputPath;

    @Value("${website.preview-output-path:./preview-websites}")
    private String previewOutputPath;

    @Value("${website.templates-path:src/main/resources/templates}")
    private String templatesPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    public Path generateWebsite(Company company, WebsiteTemplate template) {
        return generateWebsite(company, template, false);
    }

    public Path generateWebsite(Company company, WebsiteTemplate template, boolean preview) {
        try {
            String basePath = preview ? previewOutputPath : outputPath;
            // 1. 生成网站内容
            Map<String, Object> data = buildWebsiteData(company);

            // 2. 加载模板
            String templateContent = loadTemplate(template.getTemplateCode());

            // 3. 渲染模板
            String renderedHtml = renderTemplate(templateContent, data);

            // 4. 保存HTML文件
            Path websitePath = saveWebsiteFile(company.getDomain(), renderedHtml, basePath);

            // 5. 复制静态资源
            copyStaticResources(company.getDomain(), basePath);

            log.info("Successfully generated website for domain: {} (preview: {})", company.getDomain(), preview);
            return websitePath;
        } catch (Exception e) {
            log.error("Failed to generate website for domain: {} (preview: {})", company.getDomain(), preview, e);
            throw new RuntimeException("Failed to generate website: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildWebsiteData(Company company) {
        Map<String, Object> data = new HashMap<>();
        data.put("company", company);
        data.put("domain", company.getDomain());

        // 扁平化公司属性 - 用于模板替换
        data.put("company.companyName", company.getCompanyName() != null ? company.getCompanyName() : "");
        data.put("company.email", company.getEmail() != null ? company.getEmail() : "");
        data.put("company.domain", company.getDomain() != null ? company.getDomain() : "");
        data.put("company.icpNumber", company.getIcpNumber() != null ? company.getIcpNumber() : "");

        // 为兼容旧模板，添加常用别名
        data.put("company.mainTitle", company.getCompanyName() != null ? company.getCompanyName() : "");
        data.put("company.subtitle", "专业的企业服务提供商");
        data.put("company.slogan", "致力于提供最优质的产品与服务");

        // 初始化联系信息（默认值）
        String companyAddress = "上海市浦东新区张江高科技园区";
        String companyPhone = "400-123-4567";
        String companyWechat = "公司微信";

        // 获取公司内容
        List<WebsiteContent> contents = contentRepository.findByCompanyIdOrderBySortOrderAsc(company.getId());

        // 提取联系信息
        for (WebsiteContent content : contents) {
            // 从任何内容中提取联系信息
            if (content.getCompanyAddress() != null && !content.getCompanyAddress().isEmpty()) {
                companyAddress = content.getCompanyAddress();
            }
            if (content.getPhone() != null && !content.getPhone().isEmpty()) {
                companyPhone = content.getPhone();
            }
            if (content.getWechat() != null && !content.getWechat().isEmpty()) {
                companyWechat = content.getWechat();
            }

            // 按内容类型分类
            switch (content.getContentType()) {
                case "header":
                    data.put("headerContent", content);
                    // 扁平化headerContent属性
                    if (content.getTitle() != null) data.put("headerContent.title", content.getTitle());
                    if (content.getDescription() != null) data.put("headerContent.description", content.getDescription());
                    break;
                case "about":
                    data.put("aboutContent", content);
                    if (content.getDescription() != null) data.put("aboutContent.description", content.getDescription());
                    if (content.getContentDetail() != null) data.put("aboutContent.contentDetail", content.getContentDetail());
                    break;
                case "products":
                    data.put("productsContent", content);
                    if (content.getDescription() != null) data.put("productsContent.description", content.getDescription());
                    break;
                case "services":
                    data.put("servicesContent", content);
                    if (content.getDescription() != null) data.put("servicesContent.description", content.getDescription());
                    break;
                case "team":
                    data.put("teamContent", content);
                    break;
                case "news":
                    data.put("newsContent", content);
                    break;
                case "footer":
                    data.put("footerContent", content);
                    break;
                case "contact":
                    data.put("contactContent", content);
                    break;
                default:
                    data.put(content.getContentType() + "Content", content);
            }
        }

        // 添加扁平化的联系信息
        data.put("company.companyAddress", companyAddress);
        data.put("company.phone", companyPhone);
        data.put("company.wechat", companyWechat);

        // 设置默认值，确保模板中的占位符都有值
        if (!data.containsKey("aboutContent.description")) {
            data.put("aboutContent.description", "我们致力于通过创新和专业服务，为客户创造持久价值，成为行业领先的企业解决方案提供者。");
        }
        if (!data.containsKey("productsContent.description")) {
            data.put("productsContent.description", "我们的核心产品线，经过市场验证，性能稳定可靠。");
        }
        if (!data.containsKey("servicesContent.description")) {
            data.put("servicesContent.description", "我们提供全面的业务咨询和IT规划服务，帮助企业制定数字化转型战略，优化业务流程，提升运营效率。");
        }

        // 生成备案号HTML
        String icpNumber = company.getIcpNumber();
        if (icpNumber != null && !icpNumber.trim().isEmpty()) {
            data.put("company.icpNumberHtml", "<a href=\"https://beian.miit.gov.cn/\" target=\"_blank\">" + icpNumber + "</a>");
        } else {
            data.put("company.icpNumberHtml", "");
        }

        return data;
    }

    private String loadTemplate(String templateCode) throws IOException {
        Path templatePath = Paths.get(templatesPath, templateCode + ".html");

        if (!Files.exists(templatePath)) {
            throw new IOException("Template not found: " + templatePath);
        }

        return Files.readString(templatePath, StandardCharsets.UTF_8);
    }

    private String renderTemplate(String templateContent, Map<String, Object> data) {
        // 简单的模板渲染 - 使用${key}作为占位符
        String rendered = templateContent;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            rendered = rendered.replace(key, value);
        }

        return rendered;
    }

    private Path saveWebsiteFile(String domain, String htmlContent, String basePath) throws IOException {
        // 创建域名对应的目录
        Path domainPath = Paths.get(basePath, domain);
        Files.createDirectories(domainPath);

        // 保存index.html
        Path indexPath = domainPath.resolve("index.html");
        Files.writeString(indexPath, htmlContent, StandardCharsets.UTF_8);

        log.info("Website saved to: {}", indexPath.toAbsolutePath());
        return domainPath;
    }

    private Path saveWebsiteFile(String domain, String htmlContent) throws IOException {
        return saveWebsiteFile(domain, htmlContent, outputPath);
    }

    private void copyStaticResources(String domain, String basePath) throws IOException {
        // 复制静态资源到对应的域名目录
        Path staticResourcesPath = Paths.get(templatesPath, "static");
        if (Files.exists(staticResourcesPath)) {
            Path targetPath = Paths.get(basePath, domain, "static");
            copyDirectory(staticResourcesPath, targetPath);
        }
    }

    private void copyStaticResources(String domain) throws IOException {
        copyStaticResources(domain, outputPath);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);

        Files.walk(source)
                .filter(path -> !Files.isDirectory(path))
                .forEach(sourcePath -> {
                    Path relativePath = source.relativize(sourcePath);
                    Path targetPath = target.resolve(relativePath);

                    try {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log.error("Failed to copy file: {}", sourcePath, e);
                    }
                });
    }
}
