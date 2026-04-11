package com.website.controller;

import com.website.dto.LoginRequest;
import com.website.dto.LoginResponse;
import com.website.entity.User;
import com.website.security.JwtTokenUtil;
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
    private final JwtTokenUtil jwtTokenUtil;

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

            // 使用JWT生成安全的token
            String token = jwtTokenUtil.generateToken(username, user.getRole());

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
            // 使用JWT验证token
            if (jwtTokenUtil.isValidToken(token)) {
                String username = jwtTokenUtil.extractUsername(token);
                String role = jwtTokenUtil.extractRole(token);

                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent() && jwtTokenUtil.validateToken(token, username)) {
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
}