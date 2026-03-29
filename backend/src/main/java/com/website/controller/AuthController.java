package com.website.controller;

import com.website.dto.LoginRequest;
import com.website.dto.LoginResponse;
import com.website.entity.User;
import com.website.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is healthy");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            log.info("用户登录尝试: {}", username);
            log.debug("登录请求 - 用户名: {}, 密码长度: {}", username, password != null ? password.length() : 0);

            Optional<User> userOpt = userService.authenticate(username, password);
            if (userOpt.isEmpty()) {
                log.warn("用户认证失败: {}", username);
                return ResponseEntity.ok(LoginResponse.builder()
                        .success(false)
                        .message("用户名或密码错误")
                        .build());
            }

            User user = userOpt.get();
            log.info("用户登录成功: {}, 角色: {}", username, user.getRole());

            // 生成简单的token（这里可以使用JWT，简化起见使用用户名+时间戳）
            String token = generateSimpleToken(username);

            return ResponseEntity.ok(LoginResponse.builder()
                    .success(true)
                    .message("登录成功")
                    .username(username)
                    .role(user.getRole())
                    .token(token)
                    .build());

        } catch (Exception e) {
            log.error("登录处理失败", e);
            return ResponseEntity.ok(LoginResponse.builder()
                    .success(false)
                    .message("登录失败: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<LoginResponse> validateToken(@RequestParam String token) {
        try {
            // 简化验证：检查token是否有效
            // 实际应该使用JWT验证
            if (isValidToken(token)) {
                // 从token中解析用户名
                String username = extractUsernameFromToken(token);
                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    return ResponseEntity.ok(LoginResponse.builder()
                            .success(true)
                            .message("Token有效")
                            .username(user.getUsername())
                            .role(user.getRole())
                            .token(token)
                            .build());
                }
            }

            return ResponseEntity.ok(LoginResponse.builder()
                    .success(false)
                    .message("Token无效或已过期")
                    .build());

        } catch (Exception e) {
            log.error("Token验证失败", e);
            return ResponseEntity.ok(LoginResponse.builder()
                    .success(false)
                    .message("Token验证失败")
                    .build());
        }
    }

    private String generateSimpleToken(String username) {
        // 简化token生成：用户名 + 时间戳 + 随机数
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return username + "|" + timestamp + "|" + random;
    }

    private boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            String[] parts = token.split("\\|");
            if (parts.length != 3) {
                return false;
            }

            @SuppressWarnings("unused")
            String username = parts[0];
            long timestamp = Long.parseLong(parts[1]);

            // 检查token是否在24小时内生成
            long currentTime = System.currentTimeMillis();
            long tokenAge = currentTime - timestamp;
            long maxAge = 24 * 60 * 60 * 1000; // 24小时

            return tokenAge <= maxAge;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\|");
            return parts[0];
        } catch (Exception e) {
            return "";
        }
    }
}