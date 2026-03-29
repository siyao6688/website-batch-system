package com.website.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "website_templates")
public class WebsiteTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String templateName;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "preview_image")
    private String previewImage;

    @Column(name = "description")
    private String description;

    @Column(name = "main_title")
    private String mainTitle;

    @Column(name = "sub_title")
    private String subTitle;

    @Column(name = "background_image")
    private String backgroundImage;

    @Column(name = "background_color")
    private String backgroundColor = "#ffffff";

    @Column(name = "font_color")
    private String fontColor = "#333333";

    @Column(name = "layout_type")
    private String layoutType = "standard";

    @Column(name = "is_standard")
    private Boolean isStandard = false;

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
