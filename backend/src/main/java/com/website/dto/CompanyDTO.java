package com.website.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CompanyDTO {
    private Long id;
    private String name;
    private String domain;
    private Long templateId;
    private Boolean isPublished;
    private Boolean isActive;
    private LocalDateTime publishDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
