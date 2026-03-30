package com.website.service;

import com.website.entity.Company;
import com.website.entity.WebsiteContent;
import com.website.entity.WebsiteTemplate;
import com.website.deployment.ServerDeploymentService;
import com.website.generator.WebsiteGeneratorService;
import com.website.repository.CompanyRepository;
import com.website.repository.WebsiteContentRepository;
import com.website.repository.WebsiteTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final WebsiteContentRepository websiteContentRepository;
    private final WebsiteTemplateRepository templateRepository;
    private final WebsiteGeneratorService generatorService;
    private final ServerDeploymentService deploymentService;

    /**
     * 根据公司名称和域名选择模板
     * 使用公司名称+域名的哈希值来分配不同的模板，确保相同公司不同域名使用不同样式
     */
    private WebsiteTemplate selectTemplateByCompanyAndDomain(String companyName, String domain) {
        // 获取所有激活的模板
        List<WebsiteTemplate> activeTemplates = templateRepository.findByIsActive(true);
        if (activeTemplates.isEmpty()) {
            throw new EntityNotFoundException("No active template found");
        }

        // 按模板代码排序，确保一致性
        activeTemplates.sort((a, b) -> a.getTemplateCode().compareTo(b.getTemplateCode()));

        // 计算公司名称+域名的哈希值
        String combinedKey = companyName + "|" + domain;
        int hash = combinedKey.hashCode();
        // 确保哈希值为正数
        int positiveHash = hash & Integer.MAX_VALUE;
        // 根据哈希值选择模板
        int templateIndex = positiveHash % activeTemplates.size();

        WebsiteTemplate selectedTemplate = activeTemplates.get(templateIndex);
        log.info("为公司 {} 域名 {} 选择模板: {} (组合键: {}, 哈希: {}, 索引: {})",
                companyName, domain, selectedTemplate.getTemplateCode(), combinedKey, hash, templateIndex);

        return selectedTemplate;
    }

    /**
     * 根据域名选择模板（兼容旧版本）
     * 使用公司名称+域名的哈希值来分配不同的模板
     */
    private WebsiteTemplate selectTemplateByDomain(String domain) {
        // 如果没有公司名称，只使用域名（向后兼容）
        return selectTemplateByCompanyAndDomain("", domain);
    }

    public List<Company> getAllCompanies(Boolean isActive) {
        if (isActive != null) {
            return companyRepository.findByIsActiveAndIsDeletedFalse(isActive);
        }
        return companyRepository.findAllByIsDeletedFalse();
    }

    public Company getCompanyById(Long id) {
        return companyRepository.findByIdAndIsActiveAndIsDeletedFalse(id, true)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));
    }

    public Company getCompanyByDomain(String domain) {
        return companyRepository.findByDomainAndIsDeletedFalse(domain)
                .orElseThrow(() -> new EntityNotFoundException("Company not found for domain: " + domain));
    }

    @Transactional
    public Company createCompany(Company company) {
        // 确保域名以.cn结尾
        String domain = company.getDomain();
        if (domain != null && !domain.trim().isEmpty()) {
            domain = domain.trim();
            if (!domain.toLowerCase().endsWith(".cn")) {
                domain = domain + ".cn";
            }
            company.setDomain(domain);
        }

        // 首先检查是否有未删除的记录使用该域名
        Optional<Company> existingActiveCompany = companyRepository.findByDomainAndIsDeletedFalse(company.getDomain());
        if (existingActiveCompany.isPresent()) {
            throw new IllegalArgumentException("域名已被使用: " + company.getDomain() +
                    " (公司: " + existingActiveCompany.get().getCompanyName() + ")");
        }

        // 检查是否有软删除的记录使用该域名
        Optional<Company> existingDeletedCompany = companyRepository.findByDomain(company.getDomain());
        if (existingDeletedCompany.isPresent() && existingDeletedCompany.get().getIsDeleted()) {
            // 恢复软删除的记录
            Company deletedCompany = existingDeletedCompany.get();
            deletedCompany.setCompanyName(company.getCompanyName());
            deletedCompany.setEmail(company.getEmail());
            deletedCompany.setIcpNumber(company.getIcpNumber());
            deletedCompany.setHasWebsite(company.getHasWebsite());
            deletedCompany.setTemplateId(company.getTemplateId());
            deletedCompany.setIsActive(company.getIsActive());
            deletedCompany.setIsPublished(false); // 恢复后设为未发布
            deletedCompany.setPublishDate(null);
            deletedCompany.setIsDeleted(false);
            deletedCompany.setDeletedAt(null);
            deletedCompany.setUpdatedAt(LocalDateTime.now());

            log.info("恢复软删除的公司记录: {}, 域名: {}", company.getCompanyName(), company.getDomain());
            return companyRepository.save(deletedCompany);
        }

        // 检查是否有其他记录（包括未标记为删除的）使用该域名
        if (existingDeletedCompany.isPresent()) {
            // 这种情况不应该发生，但为了安全起见
            throw new IllegalArgumentException("域名冲突: " + company.getDomain() +
                    "，数据库中存在状态异常的记录");
        }

        // 创建新记录
        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompany(Long id, Company companyDetails) {
        Company company = getCompanyById(id);

        // 部分更新：只更新非null字段
        if (companyDetails.getCompanyName() != null) {
            company.setCompanyName(companyDetails.getCompanyName());
        }
        if (companyDetails.getEmail() != null) {
            company.setEmail(companyDetails.getEmail());
        }

        // 确保域名以.cn结尾（只有提供域名时才更新）
        String domain = companyDetails.getDomain();
        if (domain != null) {
            if (!domain.trim().isEmpty()) {
                domain = domain.trim();
                if (!domain.toLowerCase().endsWith(".cn")) {
                    domain = domain + ".cn";
                }
                company.setDomain(domain);
            } else {
                // 空字符串表示清除域名
                company.setDomain(null);
            }
        }
        // 如果domain为null，则不更新域名字段

        if (companyDetails.getIcpNumber() != null) {
            company.setIcpNumber(companyDetails.getIcpNumber());
        }

        // 更新模板ID和网站状态（如果提供）
        if (companyDetails.getTemplateId() != null) {
            company.setTemplateId(companyDetails.getTemplateId());
        }
        if (companyDetails.getHasWebsite() != null) {
            company.setHasWebsite(companyDetails.getHasWebsite());
        }

        return companyRepository.save(company);
    }

    /**
     * 预览网站（不部署）
     */
    @Transactional(readOnly = true)
    public Path previewWebsite(Long id) {
        Company company = getCompanyById(id);
        if (!company.getHasWebsite()) {
            throw new IllegalArgumentException("Cannot preview company without a website");
        }

        // 查找模板
        WebsiteTemplate template;
        Long templateIdToUse = company.getTemplateId();
        if (templateIdToUse != null) {
            template = templateRepository.findById(templateIdToUse)
                    .orElseThrow(() -> new EntityNotFoundException("Template not found"));
        } else {
            template = selectTemplateByCompanyAndDomain(company.getCompanyName(), company.getDomain());
            templateIdToUse = template.getId();
            log.info("预览网站使用根据公司名称和域名选择的模板: {} {} -> {}", company.getCompanyName(), company.getDomain(), template.getTemplateCode());
        }

        // 生成网站（使用预览域名，避免与正式网站冲突）
        String previewDomain = company.getDomain() + "-preview";
        Company previewCompany = new Company();
        previewCompany.setId(company.getId());
        previewCompany.setCompanyName(company.getCompanyName());
        previewCompany.setDomain(previewDomain);
        previewCompany.setEmail(company.getEmail());
        previewCompany.setIcpNumber(company.getIcpNumber());
        previewCompany.setHasWebsite(company.getHasWebsite());
        previewCompany.setTemplateId(templateIdToUse);

        // 生成网站并返回路径（预览模式）
        return generatorService.generateWebsite(previewCompany, template, true);
    }

    @Transactional
    public Company publishCompany(Long id) throws Exception {
        Company company = getCompanyById(id);
        if (!company.getHasWebsite()) {
            throw new IllegalArgumentException("Cannot publish company without a website");
        }

        // 查找模板：如果公司有指定模板ID，使用指定模板；否则根据域名选择模板
        WebsiteTemplate template;
        if (company.getTemplateId() != null) {
            template = templateRepository.findById(company.getTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("Template not found"));
        } else {
            template = selectTemplateByCompanyAndDomain(company.getCompanyName(), company.getDomain());
            // 将模板ID设置到公司记录中
            company.setTemplateId(template.getId());
        }

        // 生成网站并获取本地路径
        Path websitePath = generatorService.generateWebsite(company, template);

        // 部署网站到服务器
        try {
            deploymentService.deployWebsite(company.getDomain(), websitePath);
        } catch (Exception e) {
            log.error("部署网站到服务器失败：{}", company.getDomain(), e);
            throw new RuntimeException("部署失败：" + e.getMessage() + "。请检查服务器配置和 SSH 连接。", e);
        }

        // 生成本地Nginx配置文件（供手动部署）
        deploymentService.generateLocalNginxConfig(company.getDomain(), websitePath);

        company.setIsPublished(true);
        company.setPublishDate(LocalDateTime.now());
        company.setIsActive(true);

        return companyRepository.save(company);
    }

    @Transactional
    public Company unpublishCompany(Long id) {
        Company company = getCompanyById(id);
        company.setIsPublished(false);
        company.setPublishDate(null);
        return companyRepository.save(company);
    }

    @Transactional
    public Company toggleCompanyStatus(Long id) {
        Company company = getCompanyById(id);
        company.setIsActive(!company.getIsActive());
        return companyRepository.save(company);
    }

    public List<WebsiteTemplate> getAllTemplates(Boolean isActive) {
        if (isActive != null) {
            return templateRepository.findByIsActive(isActive);
        }
        return templateRepository.findAll();
    }

    public WebsiteTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
    }

    public List<WebsiteContent> getCompanyContents(Long companyId) {
        return websiteContentRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
    }

    @Transactional
    public WebsiteContent addContent(Long companyId, WebsiteContent content) {
        Company company = getCompanyById(companyId);
        content.setCompanyId(companyId);
        content.setCompanyName(company.getCompanyName());

        // 查找最大排序值
        List<WebsiteContent> contents = websiteContentRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
        Integer maxSort = contents.stream()
                .map(WebsiteContent::getSortOrder)
                .max(Integer::compare)
                .orElse(0);
        content.setSortOrder(maxSort + 1);

        return websiteContentRepository.save(content);
    }

    @Transactional
    public WebsiteContent updateContent(Long id, WebsiteContent contentDetails) {
        WebsiteContent content = websiteContentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content not found"));

        content.setTitle(contentDetails.getTitle());
        content.setDescription(contentDetails.getDescription());
        content.setContentDetail(contentDetails.getContentDetail());
        content.setImageUrl(contentDetails.getImageUrl());
        content.setSortOrder(contentDetails.getSortOrder());

        return websiteContentRepository.save(content);
    }

    @Transactional
    public void deleteContent(Long id) {
        if (!websiteContentRepository.existsById(id)) {
            throw new EntityNotFoundException("Content not found");
        }
        websiteContentRepository.deleteById(id);
    }

    @Transactional
    public void deleteCompany(Long id) {
        Company company = companyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        // 软删除：标记为已删除，但不取消发布、不修改域名，以保持已发布网站正常运行
        company.setIsDeleted(true);
        company.setDeletedAt(LocalDateTime.now());
        companyRepository.save(company);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new EntityNotFoundException("Template not found");
        }
        templateRepository.deleteById(id);
    }
}
