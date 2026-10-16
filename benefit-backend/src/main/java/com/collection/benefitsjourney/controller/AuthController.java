package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.LoginRequest;
import com.collection.benefitsjourney.dto.LoginResponse;
import com.collection.benefitsjourney.dto.RegisterRequest;
import com.collection.benefitsjourney.dto.UserResponse;
import com.collection.benefitsjourney.jwt.JsonWebTokenUtility;
import com.collection.benefitsjourney.service.AuthService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JsonWebTokenUtility jsonWebTokenUtility;

    public AuthController(
            AuthService authService,
            JsonWebTokenUtility jsonWebTokenUtility) {

        this.authService = authService;
        this.jsonWebTokenUtility = jsonWebTokenUtility;
    }


    // 一般會員註冊
    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request) {

        // 呼叫 AuthService 處理註冊
        UsersBean usersBean =
                authService.register(request);

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
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request , HttpServletRequest httpRequest) {

        // 1. 驗證帳號密碼
        UsersBean usersBean =
                authService.login(
                        request.getAccount(),
                        request.getPassword(),
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("User-Agent")
                );


        // 2. 建立 JWT
        String token =
                jsonWebTokenUtility.createToken(
                        usersBean.getUsersId().toString()
                );


        // 3. Entity → UserResponse
        UserResponse userResponse =
                new UserResponse();

        userResponse.setUsersId(usersBean.getUsersId());
        userResponse.setAccount(usersBean.getAccount());
        userResponse.setName(usersBean.getName());
        userResponse.setEmail(usersBean.getEmail());
        userResponse.setBirthDate(usersBean.getBirthDate());
        userResponse.setRole(usersBean.getRole());
        userResponse.setStatus(usersBean.getStatus());
        userResponse.setCreatedAt(usersBean.getCreatedAt());
        userResponse.setUpdatedAt(usersBean.getUpdatedAt());


        // 4. 建立 LoginResponse
        LoginResponse response =
                new LoginResponse();

        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUser(userResponse);

        return response;
    }


    // Google OAuth 登入
    @PostMapping("/google")
    public void googleLogin() {

    }


    // 登出
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("登出成功");
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