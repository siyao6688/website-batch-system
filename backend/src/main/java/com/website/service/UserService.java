package com.website.service;

import com.website.entity.User;
import com.website.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<User> authenticate(String username, String password) {
        log.info("尝试认证用户: {}", username);
        Optional<User> userOpt = userRepository.findByUsernameAndIsActiveTrue(username);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在或未激活: {}", username);
            return Optional.empty();
        }

        User user = userOpt.get();
        log.info("找到用户: {}, 角色: {}, 密码哈希: {}", username, user.getRole(),
                user.getPassword() != null ? user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "null");

        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        log.info("密码匹配结果: {}", passwordMatches);

        if (passwordMatches) {
            log.info("用户认证成功: {}", username);
            return Optional.of(user);
        }

        log.warn("密码不匹配用户: {}", username);
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameAndIsActiveTrue(username);
    }

    public boolean validateUser(String username, String password) {
        Optional<User> userOpt = authenticate(username, password);
        return userOpt.isPresent();
    }

    public boolean isAdmin(String username) {
        Optional<User> userOpt = userRepository.findByUsernameAndIsActiveTrue(username);
        return userOpt.isPresent() && "ADMIN".equals(userOpt.get().getRole());
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public User createUser(String username, String rawPassword, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(hashPassword(rawPassword));
        user.setRole(role != null ? role : "USER");
        user.setIsActive(true);

        return userRepository.save(user);
    }

    public void deactivateUser(Long userId) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在或已被禁用"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @PostConstruct
    public void init() {
        log.info("Testing password hash for default users...");
        String testPassword = "shuaixiaohuo";
        List<String> defaultUsers = Arrays.asList("xl", "user1");

        for (String username : defaultUsers) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean matches = passwordEncoder.matches(testPassword, user.getPassword());
                log.info("Password match for user '{}': {}", username, matches);
                if (!matches) {
                    log.info("Updating password for user '{}' to new hash...", username);
                    String newHash = passwordEncoder.encode(testPassword);
                    user.setPassword(newHash);
                    userRepository.save(user);
                    log.info("Password for user '{}' updated successfully.", username);
                }
            } else {
                log.warn("Default user '{}' not found", username);
            }
        }
    }
}