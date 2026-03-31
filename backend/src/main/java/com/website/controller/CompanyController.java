package com.website.controller;

import com.website.dto.BatchOperationRequest;
import com.website.dto.BatchOperationResult;
import com.website.entity.Company;
import com.website.entity.WebsiteContent;
import com.website.entity.WebsiteTemplate;
import com.website.service.CompanyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<?> getAllCompanies(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "all") String publishStatus,
            @RequestParam(required = false, defaultValue = "all") String websiteStatus,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            // 如果有筛选条件，使用筛选查询
            if (!"all".equals(publishStatus) || !"all".equals(websiteStatus)) {
                Page<Company> companyPage = companyService.getCompaniesWithFilters(publishStatus, websiteStatus, pageable);
                Map<String, Object> response = new HashMap<>();
                response.put("content", companyPage.getContent());
                response.put("totalElements", companyPage.getTotalElements());
                response.put("totalPages", companyPage.getTotalPages());
                response.put("currentPage", companyPage.getNumber());
                response.put("pageSize", companyPage.getSize());
                return ResponseEntity.ok(response);
            }

            // 默认查询逻辑
            if (page == 0 && size == 20 && !"id".equals(sortBy)) {
                List<Company> companies = companyService.getAllCompanies(isActive);
                return ResponseEntity.ok(companies);
            }

            Page<Company> companyPage = companyService.getCompaniesPage(isActive, pageable);
            Map<String, Object> response = new HashMap<>();
            response.put("content", companyPage.getContent());
            response.put("totalElements", companyPage.getTotalElements());
            response.put("totalPages", companyPage.getTotalPages());
            response.put("currentPage", companyPage.getNumber());
            response.put("pageSize", companyPage.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get companies", e);
            return ResponseEntity.badRequest().body("Failed to get companies: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
        try {
            Company company = companyService.getCompanyById(id);
            return ResponseEntity.ok(company);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get company by id", e);
            return ResponseEntity.badRequest().body("Failed to get company: " + e.getMessage());
        }
    }

    @GetMapping("/domain/{domain}")
    public ResponseEntity<?> getCompanyByDomain(@PathVariable String domain) {
        try {
            Company company = companyService.getCompanyByDomain(domain);
            return ResponseEntity.ok(company);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get company by domain", e);
            return ResponseEntity.badRequest().body("Failed to get company: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createCompany(@RequestBody Company company) {
        try {
            Company createdCompany;
            if (company.getHasWebsite() != null && company.getHasWebsite()) {
                // 如果勾选了搭建官网，创建公司并自动生成部署网站
                createdCompany = companyService.createCompanyWithWebsite(company);
            } else {
                createdCompany = companyService.createCompany(company);
            }
            return ResponseEntity.ok(createdCompany);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create company", e);
            return ResponseEntity.badRequest().body("Failed to create company: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @RequestBody Company companyDetails) {
        try {
            Company updatedCompany = companyService.updateCompany(id, companyDetails);
            return ResponseEntity.ok(updatedCompany);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update company", e);
            return ResponseEntity.badRequest().body("Failed to update company: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishCompany(@PathVariable Long id) {
        try {
            Company company = companyService.publishCompany(id);
            return ResponseEntity.ok(company);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to publish company", e);
            return ResponseEntity.badRequest().body("Failed to publish company: " + e.getMessage());
        }
    }

    @PostMapping("/batch/publish")
    public ResponseEntity<?> batchPublish(@RequestBody BatchOperationRequest request) {
        try {
            if (request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseEntity.badRequest().body("请选择要发布的公司");
            }
            if (request.getIds().size() > 100) {
                return ResponseEntity.badRequest().body("一次最多只能发布100条数据，当前选择了 " + request.getIds().size() + " 条");
            }
            BatchOperationResult result = companyService.batchPublish(request.getIds(), false);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to batch publish companies", e);
            return ResponseEntity.badRequest().body("批量发布失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/republish")
    public ResponseEntity<?> republishCompany(@PathVariable Long id) {
        try {
            Company company = companyService.republishCompany(id);
            return ResponseEntity.ok(company);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to republish company", e);
            return ResponseEntity.badRequest().body("重新发布失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch/republish")
    public ResponseEntity<?> batchRepublish(@RequestBody BatchOperationRequest request) {
        try {
            if (request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseEntity.badRequest().body("请选择要重新发布的公司");
            }
            if (request.getIds().size() > 100) {
                return ResponseEntity.badRequest().body("一次最多只能发布100条数据，当前选择了 " + request.getIds().size() + " 条");
            }
            BatchOperationResult result = companyService.batchPublish(request.getIds(), true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to batch republish companies", e);
            return ResponseEntity.badRequest().body("批量重新发布失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/check-status")
    public ResponseEntity<?> checkCompanyStatus(@PathVariable Long id) {
        try {
            Company company = companyService.checkCompanyStatus(id);
            return ResponseEntity.ok(company);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to check company status", e);
            return ResponseEntity.badRequest().body("检测失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch/check-status")
    public ResponseEntity<?> batchCheckStatus(@RequestBody BatchOperationRequest request) {
        try {
            if (request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseEntity.badRequest().body("请选择要检测的公司");
            }
            if (request.getIds().size() > 100) {
                return ResponseEntity.badRequest().body("一次最多只能检测100条数据，当前选择了 " + request.getIds().size() + " 条");
            }
            BatchOperationResult result = companyService.batchCheckStatus(request.getIds());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to batch check status", e);
            return ResponseEntity.badRequest().body("批量检测失败：" + e.getMessage());
        }
    }

    @PostMapping("/check-all-status")
    public ResponseEntity<?> checkAllStatus() {
        try {
            int problemCount = companyService.checkAllPublishedStatus();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "检测完成");
            result.put("problemCount", problemCount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to check all status", e);
            return ResponseEntity.badRequest().body("检测失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch/generate")
    public ResponseEntity<?> batchGenerate(@RequestBody BatchOperationRequest request) {
        try {
            if (request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseEntity.badRequest().body("请选择要生成的公司");
            }
            if (request.getIds().size() > 100) {
                return ResponseEntity.badRequest().body("一次最多只能生成100条数据，当前选择了 " + request.getIds().size() + " 条");
            }
            BatchOperationResult result = companyService.batchGenerate(request.getIds());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to batch generate websites", e);
            return ResponseEntity.badRequest().body("批量生成失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleCompanyStatus(@PathVariable Long id) {
        try {
            Company company = companyService.toggleCompanyStatus(id);
            return ResponseEntity.ok(company);
        } catch (Exception e) {
            log.error("Failed to toggle company status", e);
            return ResponseEntity.badRequest().body("Failed to toggle company status: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<?> previewWebsite(@PathVariable Long id) {
        try {
            Path websitePath = companyService.previewWebsite(id);
            // 构建预览URL
            String previewDomain = websitePath.getFileName().toString(); // 目录名即域名-preview
            String previewUrl = "/preview/" + previewDomain + "/index.html";
            return ResponseEntity.ok(new PreviewResponse(previewUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to preview website", e);
            return ResponseEntity.badRequest().body("Failed to preview website: " + e.getMessage());
        }
    }

    // 预览响应类
    private static class PreviewResponse {
        private final String previewUrl;

        public PreviewResponse(String previewUrl) {
            this.previewUrl = previewUrl;
        }

        public String getPreviewUrl() {
            return previewUrl;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        try {
            companyService.deleteCompany(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete company", e);
            return ResponseEntity.badRequest().body("Failed to delete company: " + e.getMessage());
        }
    }
}
