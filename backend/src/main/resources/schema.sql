-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS website_batch_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE website_batch_system;

-- 创建公司表
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    serial_number INT COMMENT '序号（Excel行号）',
    company_name VARCHAR(200) NOT NULL COMMENT '公司名称',
    email VARCHAR(100) COMMENT '邮箱',
    domain VARCHAR(100) NOT NULL UNIQUE COMMENT '域名',
    icp_number VARCHAR(50) COMMENT '备案号',
    has_website BOOLEAN DEFAULT FALSE COMMENT '是否搭建官网',
    template_id BIGINT COMMENT '模板ID',
    is_published BOOLEAN DEFAULT FALSE COMMENT '是否发布',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    publish_date DATETIME COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_domain (domain),
    INDEX idx_company_name (company_name),
    INDEX idx_is_active (is_active),
    INDEX idx_is_published (is_published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司信息表';

-- 创建网站内容表
CREATE TABLE IF NOT EXISTS website_contents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    company_id BIGINT NOT NULL COMMENT '公司ID',
    company_name VARCHAR(200) NOT NULL COMMENT '公司名称',
    content_type VARCHAR(50) NOT NULL COMMENT '内容类型',
    title VARCHAR(200) COMMENT '标题',
    description TEXT COMMENT '描述',
    content_detail TEXT COMMENT '内容详情',
    image_url VARCHAR(500) COMMENT '图片URL',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_company_id (company_id),
    INDEX idx_company_type (company_id, content_type),
    INDEX idx_sort_order (company_id, sort_order),
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网站内容表';

-- 创建网站模板表
CREATE TABLE IF NOT EXISTS website_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板代码',
    preview_image VARCHAR(500) COMMENT '预览图',
    description TEXT COMMENT '描述',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_code (template_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网站模板表';

-- 插入默认模板
INSERT INTO website_templates (template_name, template_code, description) VALUES
('标准模板', 'standard', '标准公司网站模板'),
('简约模板', 'minimal', '简约风格公司网站模板'),
('商务模板', 'business', '商务风格公司网站模板');

SET FOREIGN_KEY_CHECKS = 1;
