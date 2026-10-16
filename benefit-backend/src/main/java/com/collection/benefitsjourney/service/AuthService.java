package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.ChangePasswordRequest;
import com.collection.benefitsjourney.dto.RegisterRequest;
import com.collection.benefitsjourney.repository.UsersRepository;

<<<<<<< HEAD
        @Service
        public class AuthService {
        private final UsersRepository usersRepository;
        private final PasswordEncoder passwordEncoder;
        private final LoginLogService loginLogService;
=======
@Service
public class AuthService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogService loginLogService;
>>>>>>> c52e5de (﻿feat: add Google ID token verification (有修改application.properties及加入 Google API 依賴))

        public AuthService(
                UsersRepository usersRepository,
                PasswordEncoder passwordEncoder,
                LoginLogService loginLogService) {

                this.usersRepository = usersRepository;
                this.passwordEncoder = passwordEncoder;
                this.loginLogService = loginLogService;
        }

        /**
         * 一般會員註冊
         */
        public UsersBean register(RegisterRequest request) {

                // 檢查帳號是否重複
                if (usersRepository.existsByAccount(request.getAccount())) {
                throw new IllegalArgumentException("帳號已存在");
                }

                // 檢查 Email 是否重複
                if (usersRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email 已存在");
                }

        // 建立會員
        UsersBean usersBean = new UsersBean();

        usersBean.setAccount(request.getAccount());

        // 使用 BCrypt 加密密碼
        usersBean.setPassword(
                passwordEncoder.encode(request.getPassword()));

        usersBean.setName(request.getName());
        usersBean.setEmail(request.getEmail());
        usersBean.setBirthDate(request.getBirthDate());

        // 預設一般會員
        usersBean.setRole("USER");

        // 預設啟用
        usersBean.setStatus("ACTIVE");

        // 建立 / 修改時間
        LocalDateTime now = LocalDateTime.now();
        usersBean.setCreatedAt(now);
        usersBean.setUpdatedAt(now);

        return usersRepository.save(usersBean);
    }

    /**
     * 一般帳號密碼登入
     */
    public UsersBean login(String account, String password, String ipAddress, String userAgent) {

        // 根據帳號尋找會員
        Optional<UsersBean> optionalUser = usersRepository.findByAccount(account);

        // 找不到帳號 記錄失敗，user_id 為 NULL
        if (optionalUser.isEmpty()) {
            loginLogService.saveLoginLog(
                    null,
                    "FAILED",
                    ipAddress,
                    userAgent);
            throw new IllegalArgumentException("帳號或密碼錯誤");
        }

        UsersBean usersBean = optionalUser.get();

        // 被管理員停權、註銷的帳號不能登入：記錄失敗
        if ("BANNED".equals(usersBean.getStatus()) || "DELETED".equals(usersBean.getStatus())) {
            loginLogService.saveLoginLog(
                    usersBean,
                    "FAILED",
                    ipAddress,
                    userAgent);
            throw new IllegalArgumentException("此帳號目前無法登入");
        }

        // Google 會員可能沒有本地密碼
        // 使用 BCrypt 驗證密碼 密碼錯誤：記錄失敗
        if (usersBean.getPassword() == null
                || !passwordEncoder.matches(
                        password,
                        usersBean.getPassword())) {
            loginLogService.saveLoginLog(
                    usersBean,
                    "FAILED",
                    ipAddress,
                    userAgent);
            throw new IllegalArgumentException("帳號或密碼錯誤");
        }

        // 登入成功：記錄登入紀錄
        // 不更新 updatedAt，因為會員資料沒有被修改
        loginLogService.saveLoginLog(
                usersBean,
                "SUCCESS",
                ipAddress,
                userAgent);

        return usersBean;

    }

    /**
     * 修改目前登入會員密碼
     *
     * 需要：
     * 1. JWT 驗證成功
     * 2. 目前密碼正確
     * 3. 新密碼不能與目前密碼相同
     */
    public void changePassword(
            Integer usersId,
            ChangePasswordRequest request) {

        // 根據 JWT 中的 usersId 找會員
        UsersBean usersBean = usersRepository.findById(usersId)
                .orElseThrow(() -> new IllegalArgumentException("找不到會員"));

        // Google 會員沒有本地密碼時，不能用目前密碼驗證
        if (usersBean.getPassword() == null) {
            throw new IllegalArgumentException(
                    "未設定本地密碼");
        }

        // 確認目前密碼是否正確
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                usersBean.getPassword())) {

            throw new IllegalArgumentException("目前密碼錯誤");
        }

        // 確認新密碼不能與目前密碼相同
        if (passwordEncoder.matches(
                request.getNewPassword(),
                usersBean.getPassword())) {

            throw new IllegalArgumentException(
                    "新密碼不能與目前密碼相同");
        }

        // 使用 BCrypt 加密新密碼
        usersBean.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()));

        // 更新修改時間
        usersBean.setUpdatedAt(LocalDateTime.now());

        // 儲存會員資料
        usersRepository.save(usersBean);
        }

<<<<<<< HEAD
        /**
         * Google OAuth 登入
         *
         * @param googleId googleId、email、name 必須來自後端已驗證的 Google ID Token，
         * 不能直接使用前端自行傳來、尚未驗證的資料。
         */
        public UsersBean googleLogin(
                String googleId,
                String email,
                String name) {
=======
    /**
     * Google OAuth 登入
     *
     * @param googleId googleId、email、name 必須來自後端已驗證的 Google ID Token，
     * 不能直接使用前端自行傳來、尚未驗證的資料。
     */
    public UsersBean googleLogin(
            String googleId,
            String email,
            String name) {
>>>>>>> c52e5de (﻿feat: add Google ID token verification (有修改application.properties及加入 Google API 依賴))

        /*
         * ① 先確認 Google 帳號是否註冊
         */
        Optional<UsersBean> googleUser =
                usersRepository.findByGoogleId(googleId);

        if (googleUser.isPresent()) {

        UsersBean usersBean = googleUser.get();

            // 停權或已註銷的帳號不能登入
        if ("BANNED".equals(usersBean.getStatus())
                || "DELETED".equals(usersBean.getStatus())) {

                throw new IllegalArgumentException(
                        "此帳號目前無法登入"
                );
        }

            // 已存在的 Google 會員直接回傳
            // 不更新 updatedAt，因為沒有修改會員資料
        return usersBean;
        }


        // 2. Google ID 不存在，檢查 Email 是否已被使用
        // 不自動將 Google 帳號綁定到一般會員
                if (usersRepository.existsByEmail(email)) {

                throw new IllegalArgumentException(
                        "此 Email 已註冊，請使用一般帳號登入"
                );
        }

        // 3. Email 尚未使用，建立新的 Google 會員
        UsersBean newUser = new UsersBean();

        // 系統自動產生唯一帳號
        String account;

        do {
<<<<<<< HEAD
        account = "G_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
=======
            account = "G_" + UUID.randomUUID()
                    .toString()
                    .replace("-", "");
>>>>>>> c52e5de (﻿feat: add Google ID token verification (有修改application.properties及加入 Google API 依賴))
        } while (usersRepository.existsByAccount(account));

        newUser.setAccount(account);

        // Google 會員不需要本地密碼
        newUser.setPassword(null);

        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setGoogleId(googleId);
        // Google 會員生日可以先不填
        newUser.setBirthDate(null);

        // 預設一般會員
        newUser.setRole("USER");

        // 預設啟用
        newUser.setStatus("ACTIVE");

        // 建立時間與資料修改時間
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        return usersRepository.save(newUser);
        }
}
