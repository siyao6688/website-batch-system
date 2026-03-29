package com.website.controller;

import com.website.entity.WebsiteContent;
import com.website.service.CompanyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
public class ContentController {

    private final CompanyService companyService;

    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getCompanyContents(@PathVariable Long companyId) {
        try {
            List<WebsiteContent> contents = companyService.getCompanyContents(companyId);
            return ResponseEntity.ok(contents);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get company contents", e);
            return ResponseEntity.badRequest().body("Failed to get contents: " + e.getMessage());
        }
    }

    @PostMapping("/company/{companyId}")
    public ResponseEntity<?> addContent(
            @PathVariable Long companyId,
            @RequestBody WebsiteContent content) {
        try {
            WebsiteContent createdContent = companyService.addContent(companyId, content);
            return ResponseEntity.ok(createdContent);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to add content", e);
            return ResponseEntity.badRequest().body("Failed to add content: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContent(@PathVariable Long id, @RequestBody WebsiteContent contentDetails) {
        try {
            WebsiteContent updatedContent = companyService.updateContent(id, contentDetails);
            return ResponseEntity.ok(updatedContent);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to update content", e);
            return ResponseEntity.badRequest().body("Failed to update content: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContent(@PathVariable Long id) {
        try {
            companyService.deleteContent(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete content", e);
            return ResponseEntity.badRequest().body("Failed to delete content: " + e.getMessage());
        }
    }
}
