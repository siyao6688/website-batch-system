-- 插入默认用户（密码使用BCrypt加密: shuaixiaohuo）
INSERT IGNORE INTO users (username, password, role, is_active) VALUES
('xl', '$2a$10$3ZRQyA/JiXmXWKL.Hc9p.uIjI8u6uZK7OSoqv8sEdYpP4hMEJ3RzW', 'ADMIN', TRUE),
('user1', '$2a$10$3ZRQyA/JiXmXWKL.Hc9p.uIjI8u6uZK7OSoqv8sEdYpP4hMEJ3RzW', 'USER', TRUE);