package com.website.controller;

import com.website.entity.Company;
import com.website.service.CompanyService;
import com.website.util.ExcelParser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/excel")
@RequiredArgsConstructor
public class ExcelUploadController {

    private final ExcelParser excelParser;
    private final CompanyService companyService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received Excel file upload: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

            if (file.isEmpty()) {
                log.warn("Uploaded file is empty");
                return ResponseEntity.badRequest().body("上传的文件为空");
            }

            // 检查文件类型
            String contentType = file.getContentType();
            log.info("File content type: {}", contentType);
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }
            boolean validExtension = extension.equals(".xls") || extension.equals(".xlsx") || extension.equals(".csv");
            if (contentType == null || !(contentType.equals("application/vnd.ms-excel") ||
                    contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                    contentType.equals("text/csv") ||
                    contentType.equals("application/csv"))) {
                // If content type is not allowed, check extension
                if (!validExtension) {
                    log.warn("Unsupported file type: {}, extension: {}", contentType, extension);
                    return ResponseEntity.badRequest().body("不支持的文件类型: " + contentType + "，请上传Excel或CSV文件");
                }
            }

            // 解析Excel文件
            List<Company> companies = excelParser.parseExcel(file);
            log.info("Successfully parsed {} companies from Excel", companies.size());

            if (companies.isEmpty()) {
                log.warn("No companies found in Excel file");
                return ResponseEntity.badRequest().body("Excel文件中未找到有效数据，请检查文件格式");
            }

            // 保存到数据库
            int successCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < companies.size(); i++) {
                Company company = companies.get(i);
                try {
                    // 创建公司并自动生成网站（不发布）
                    companyService.createCompanyWithWebsite(company);
                    successCount++;
                    log.debug("Successfully created company with website: {}", company.getCompanyName());
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format("第%d行公司'%s'导入失败: %s",
                            i + 2, company.getCompanyName(), e.getMessage());
                    errors.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }

            if (errorCount > 0) {
                String message = String.format("成功导入%d条记录，失败%d条记录。错误详情：%s",
                        successCount, errorCount, String.join("; ", errors));
                return ResponseEntity.status(207).body(message); // 207 Multi-Status
            }

            log.info("Successfully imported {} companies with auto-generated websites", successCount);
            return ResponseEntity.ok("成功导入 " + successCount + " 家公司信息，已自动生成网站（未发布）");
        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            return ResponseEntity.badRequest().body("Excel文件解析失败: " + e.getMessage() +
                    "，请确保文件格式正确且包含正确的表头（公司名称、邮箱、域名、备案号）");
        } catch (Exception e) {
            log.error("Failed to upload Excel file", e);
            return ResponseEntity.badRequest().body("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/companies")
    public ResponseEntity<?> getAllCompanies(
            @RequestParam(required = false) Boolean isActive) {
        try {
            List<Company> companies = companyService.getAllCompanies(isActive);
            log.info("Successfully retrieved {} companies", companies.size());
            return ResponseEntity.ok(companies);
        } catch (Exception e) {
            log.error("Failed to get companies", e);
            return ResponseEntity.status(500).body("Failed to get companies: " + e.getMessage());
        }
    }

    @GetMapping("/companies/{id}")
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

    @GetMapping("/companies/domain/{domain}")
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
}
