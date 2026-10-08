package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.RegisterRequest;
import com.collection.benefitsjourney.repository.UsersRepository;

import java.util.Optional;

@Service 
public class AuthService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder) {

        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
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
                passwordEncoder.encode(request.getPassword())
        );

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
    public UsersBean login(String account, String password) {

    // 根據帳號尋找會員
    Optional<UsersBean> optionalUser =
            usersRepository.findByAccount(account);

    // 找不到帳號
    if (optionalUser.isEmpty()) {
        throw new IllegalArgumentException("帳號或密碼錯誤");
    }

    UsersBean usersBean = optionalUser.get();

    // 確認會員狀態
    if (!"ACTIVE".equals(usersBean.getStatus())) {
        throw new IllegalArgumentException("此帳號目前無法登入");
    }

    // 使用 BCrypt 驗證密碼
    if (!passwordEncoder.matches(password, usersBean.getPassword())) {
        throw new IllegalArgumentException("帳號或密碼錯誤");
    }

    // 更新修改時間
    usersBean.setUpdatedAt(LocalDateTime.now());

    return usersRepository.save(usersBean);
}

    /**
     * Google OAuth 登入
     *
     * @param googleId Google 提供的唯一使用者 ID
     * @param email    Google 帳號 Email
     * @param name     Google 帳號名稱
     */
    public UsersBean googleLogin(
            String googleId,
            String email,
            String name) {

        /*
         * ① 先確認 Google 帳號是否已經綁定會員
         */
        Optional<UsersBean> googleUser =
                usersRepository.findByGoogleId(googleId);

        if (googleUser.isPresent()) {

            UsersBean usersBean = googleUser.get();

            // 確認帳號目前仍是啟用狀態
            if (!"ACTIVE".equals(usersBean.getStatus())) {
                throw new IllegalArgumentException("此帳號目前無法登入");
            }

            return usersBean;
        }
    
        /*
         * ② Google 帳號第一次登入
         *
         *    Google ID 找不到時，
         *    再確認 Email 是否已經有會員
         */
        Optional<UsersBean> emailUser =
                usersRepository.findByEmail(email);

        if (emailUser.isPresent()) {

        /*
         * Email 已經存在
         *
         * → 表示這個人原本已經有一般會員帳號
         * → 將 Google 帳號綁定到原本會員
         */
        UsersBean usersBean = emailUser.get();

        // 如果原本已經有其他 Google 帳號
        if (usersBean.getGoogleId() != null) {
            throw new IllegalArgumentException(
                    "已經綁定其他 Google 帳號"
            );
        }

            // 確認帳號目前仍是啟用狀態
        if (!"ACTIVE".equals(usersBean.getStatus())) {
            throw new IllegalArgumentException("此帳號目前無法登入");
        }

        usersBean.setGoogleId(googleId);
        usersBean.setUpdatedAt(LocalDateTime.now());
        return usersRepository.save(usersBean);
    }

        /*
         * ③ 第一次登入且 Email 也尚未註冊
         *
         * → 建立新的會員帳號，並綁定 Google ID
         */
        UsersBean newUser = new UsersBean();
        newUser.setAccount(email);
        newUser.setPassword(passwordEncoder.encode("google-oauth"));
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setGoogleId(googleId);
        newUser.setRole("USER");
        newUser.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        return usersRepository.save(newUser);
    }
}
