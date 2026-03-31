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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<?> getAllCompanies(
            @RequestParam(required = false) Boolean isActive) {
        try {
            List<Company> companies = companyService.getAllCompanies(isActive);
            return ResponseEntity.ok(companies);
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
            BatchOperationResult result = companyService.batchPublish(request.getIds());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to batch publish companies", e);
            return ResponseEntity.badRequest().body("批量发布失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch/generate")
    public ResponseEntity<?> batchGenerate(@RequestBody BatchOperationRequest request) {
        try {
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
