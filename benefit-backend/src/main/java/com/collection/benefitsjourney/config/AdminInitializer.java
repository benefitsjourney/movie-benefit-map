package com.collection.benefitsjourney.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.UsersRepository;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-init.account:}")
    private String adminAccount;

    @Value("${app.admin-init.password:}")
    private String adminPassword;

    @Value("${app.admin-init.email:}")
    private String adminEmail;

    public AdminInitializer(
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // 尚未設定初始管理員資料，就不建立帳號
        if (adminAccount.isBlank()
                || adminPassword.isBlank()
                || adminEmail.isBlank()) {
            return;
        }

        // 初始密碼至少 8 碼
        if (adminPassword.length() < 8
                || adminPassword.length() > 16) {
            throw new IllegalStateException(
                    "初始管理員密碼必須為 8～16 碼");
        }

        // 已有管理員就不重複建立
        if (usersRepository.existsByRole("ADMIN")) {
            return;
        }

        // 避免與現有會員帳號或 Email 衝突
        if (usersRepository.existsByAccount(adminAccount)
                || usersRepository.existsByEmail(adminEmail)) {
            throw new IllegalStateException(
                    "初始管理員帳號或 Email 已被使用，請檢查設定");
        }

        UsersBean admin = new UsersBean();

        admin.setAccount(adminAccount);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setName(adminAccount);
        admin.setEmail(adminEmail);
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);

        usersRepository.save(admin);

        System.out.println("初始管理員帳號已建立。");
    }
}
