package com.website.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "website_contents")
public class WebsiteContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "company_address")
    private String companyAddress;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "wechat")
    private String wechat;

    @Column(name = "company_history", columnDefinition = "TEXT")
    private String companyHistory;

    @Column(name = "company_culture", columnDefinition = "TEXT")
    private String companyCulture;

    @Column(name = "products", columnDefinition = "TEXT")
    private String products;

    @Column(name = "services", columnDefinition = "TEXT")
    private String services;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "title")
    private String title;

    @Column(name = "content_detail")
    private String contentDetail;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
