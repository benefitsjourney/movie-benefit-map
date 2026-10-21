package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.GoogleLoginRequest;
import com.collection.benefitsjourney.dto.LoginRequest;
import com.collection.benefitsjourney.dto.LoginResponse;
import com.collection.benefitsjourney.dto.RegisterRequest;
import com.collection.benefitsjourney.dto.UserResponse;
import com.collection.benefitsjourney.jwt.JsonWebTokenUtility;
import com.collection.benefitsjourney.service.AuthService;
import com.collection.benefitsjourney.service.GoogleTokenService;
import com.collection.benefitsjourney.dto.ForgotPasswordRequest;
import com.collection.benefitsjourney.dto.ResetPasswordRequest;
import com.collection.benefitsjourney.service.PasswordResetService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

        @RestController
        @RequestMapping("/auth")
        public class AuthController {

        private final AuthService authService;
        private final JsonWebTokenUtility jsonWebTokenUtility;
        private final GoogleTokenService googleTokenService;
        private final PasswordResetService passwordResetService;

        public AuthController(
                AuthService authService,
                JsonWebTokenUtility jsonWebTokenUtility,
                GoogleTokenService googleTokenService,
                PasswordResetService passwordResetService) {

                this.authService = authService;
                this.jsonWebTokenUtility = jsonWebTokenUtility;
                this.googleTokenService = googleTokenService;
                this.passwordResetService = passwordResetService;
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
        public LoginResponse googleLogin( @Valid @RequestBody GoogleLoginRequest request ) {
        // 1. 驗證 Google ID Token
        GoogleIdToken.Payload payload =
                googleTokenService.verify(request.getIdToken());

        // 2. 取得經過驗證的 Google 會員資料
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        if (googleId == null || email == null || email.isBlank()) {
                throw new IllegalArgumentException(
                        "Google 帳號缺少必要的會員資料");
        }

        // 3. 查詢既有 Google 會員，或建立新會員
        UsersBean usersBean =
                authService.googleLogin(googleId, email, name);

        // 4. 產生本系統自己的 JWT
        String token =
                jsonWebTokenUtility.createToken(
                        usersBean.getUsersId().toString());

        // 5. Entity → UserResponse
        UserResponse userResponse = new UserResponse();

        userResponse.setUsersId(usersBean.getUsersId());
        userResponse.setAccount(usersBean.getAccount());
        userResponse.setName(usersBean.getName());
        userResponse.setEmail(usersBean.getEmail());
        userResponse.setBirthDate(usersBean.getBirthDate());
        userResponse.setRole(usersBean.getRole());
        userResponse.setStatus(usersBean.getStatus());
        userResponse.setCreatedAt(usersBean.getCreatedAt());
        userResponse.setUpdatedAt(usersBean.getUpdatedAt());

        // 6. 沿用原本的 LoginResponse 格式
        LoginResponse response = new LoginResponse();

        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUser(userResponse);

        return response;

        }


        // 登出
        @PostMapping("/logout")
        public ResponseEntity<String> logout() {
                return ResponseEntity.ok("登出成功");
        }


        // 忘記密碼
        @PostMapping("/password/forgot")
        public ResponseEntity<Map<String, String>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "如果此 Email 已註冊，系統將寄送密碼重設連結"));
        }


        // 重設密碼
        @PostMapping("/password/reset")
        public ResponseEntity<Map<String, String>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "message", "密碼重設成功，請使用新密碼登入"));
        }
}