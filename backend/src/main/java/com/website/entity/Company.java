package com.website.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 序号（Excel中的行号，不需要存储）
    private Integer serialNumber;

    // 公司名称
    @Column(name = "company_name")
    private String companyName;

    // 邮箱
    @Column(name = "email")
    private String email;

    // 域名
    @Column(name = "domain")
    private String domain;

    // 备案号
    @Column(name = "icp_number")
    private String icpNumber;

    // 是否搭建官网
    @Column(name = "has_website")
    private Boolean hasWebsite = false;

    // 模板ID
    @Column(name = "template_id")
    private Long templateId;

    // 是否发布
    @Column(name = "is_published")
    private Boolean isPublished = false;

    // 是否激活
    @Column(name = "is_active")
    private Boolean isActive = true;

    // 发布时间
    @Column(name = "publish_date")
    private LocalDateTime publishDate;

    // 创建时间
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 是否已删除（软删除）
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // 删除时间
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
