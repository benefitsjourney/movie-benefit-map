package com.collection.benefitsjourney.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.PasswordResetTokenBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.PasswordResetTokenRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@Service
public class PasswordResetService {

    private final UsersRepository usersRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.password-reset-url:http://localhost:5173/reset-password}")
    private String passwordResetUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UsersRepository usersRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender) {

        this.usersRepository = usersRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    // 申請重設密碼
    @Transactional
    public void forgotPassword(String email) {

        usersRepository.findByEmail(email).ifPresent(user -> {

            // 只替啟用中的會員寄送
            if (!"ACTIVE".equals(user.getStatus())) {
                return;
            }

            // 讓之前尚未使用的 Token 失效
            List<PasswordResetTokenBean> oldTokens =
                    tokenRepository.findAllByUsersIdAndUsedFalse(user.getUsersId());

            oldTokens.forEach(token -> token.setUsed(true));
            tokenRepository.saveAll(oldTokens);

            // 產生高強度隨機 Token
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);

            String rawToken = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes);

            // 資料庫只保存 SHA-256 雜湊值
            PasswordResetTokenBean resetToken = new PasswordResetTokenBean();
            resetToken.setUsersId(user.getUsersId());
            resetToken.setTokenHash(hashToken(rawToken));
            resetToken.setCreatedAt(LocalDateTime.now());
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            resetToken.setUsed(false);

            tokenRepository.save(resetToken);

            // 寄送重設連結
            String resetLink = passwordResetUrl + "?token=" + rawToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(user.getEmail());
            message.setSubject("密碼重設通知");
            message.setText(
                    "您好：\n\n"
                    + "我們收到您的密碼重設申請。\n"
                    + "請點擊以下連結設定新密碼：\n\n"
                    + resetLink + "\n\n"
                    + "此連結將於 15 分鐘後失效，且只能使用一次。\n"
                    + "如果不是您本人提出申請，請忽略此郵件。");

            mailSender.send(message);
        });
    }

    // 驗證 Token 並重設密碼
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {

        String tokenHash = hashToken(rawToken);

        PasswordResetTokenBean resetToken = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> invalidToken());

        if (resetToken.isUsed()
                || !resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw invalidToken();
        }

        UsersBean user = usersRepository.findById(resetToken.getUsersId())
                .orElseThrow(() -> invalidToken());

        // 不允許將密碼重設為目前相同的密碼
        if (user.getPassword() != null
                && passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "新密碼不可與目前密碼相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        // 使用成功後，將此會員所有未使用的重設 Token 失效
        List<PasswordResetTokenBean> tokens =
                tokenRepository.findAllByUsersIdAndUsedFalse(user.getUsersId());

        tokens.forEach(token -> token.setUsed(true));

        usersRepository.save(user);
        tokenRepository.saveAll(tokens);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("無法執行 Token 雜湊", e);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "重設連結無效、已過期或已使用");
    }
}