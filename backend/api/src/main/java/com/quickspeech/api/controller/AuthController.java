package com.quickspeech.api.controller;

import com.quickspeech.common.entity.ApiResponse;
import com.quickspeech.common.entity.User;
import com.quickspeech.common.repository.UserRepository;
import com.quickspeech.common.util.JwtUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnBean(UserRepository.class)
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(
                user.getId(), user.getTenantId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("tenantId", user.getTenantId());

        return ApiResponse.success(result);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");

        if (userRepository.existsByUsernameAndDeletedFalse(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setTenantId(1L); // 默认租户
        user.setRole("USER");
        user.setStatus("ACTIVE");

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getId(), user.getTenantId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());

        return ApiResponse.success(result);
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserId(token);
        Long tenantId = jwtUtil.getTenantId(token);
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        String newToken = jwtUtil.generateToken(userId, tenantId, username, role);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        return ApiResponse.success(result);
    }
}
