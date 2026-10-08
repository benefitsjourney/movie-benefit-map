package com.collection.benefitsjourney.controller;

import org.springframework.web.bind.annotation.*;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.RegisterRequest;
import com.collection.benefitsjourney.dto.UserResponse;
import com.collection.benefitsjourney.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 一般會員註冊
    @PostMapping("/register")
    public UserResponse register( @Valid @RequestBody RegisterRequest request ) {
        // 呼叫 AuthService 處理註冊 
        UsersBean usersBean = authService.register(request);

        // Entity → Response DTO
        UserResponse response = new UserResponse();
        response.setUsersId(usersBean.getUsersId());
        response.setAccount(usersBean.getAccount());
        response.setName(usersBean.getName());
        response.setEmail(usersBean.getEmail());
        response.setBirthDate(usersBean.getBirthDate());
        response.setRole(usersBean.getRole());
        response.setStatus(usersBean.getStatus());
        response.setCreatedAt(usersBean.getCreatedAt());
        response.setUpdatedAt(usersBean.getUpdatedAt());
        return response;
    }

    // 一般帳號密碼登入
    @PostMapping("/login")
    public void login() {

    }

    // Google OAuth 登入
    @PostMapping("/google")
    public void googleLogin() {

    }

    // 登出
    @PostMapping("/logout")
    public void logout() {

    }

    // 忘記密碼
    @PostMapping("/password/forgot")
    public void forgotPassword() {

    }

    // 重設密碼
    @PostMapping("/password/reset")
    public void resetPassword() {

    }
}