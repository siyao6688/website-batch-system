package com.website.controller;

import com.website.entity.WebsiteTemplate;
import com.website.service.CompanyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<?> getAllTemplates(
            @RequestParam(required = false) Boolean isActive) {
        try {
            List<WebsiteTemplate> templates = companyService.getAllTemplates(isActive);
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Failed to get templates", e);
            return ResponseEntity.badRequest().body("Failed to get templates: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        try {
            WebsiteTemplate template = companyService.getTemplateById(id);
            return ResponseEntity.ok(template);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get template by id", e);
            return ResponseEntity.badRequest().body("Failed to get template: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            companyService.deleteTemplate(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete template", e);
            return ResponseEntity.badRequest().body("Failed to delete template: " + e.getMessage());
        }
    }
}
