package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetTokenBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // 對應會員編號
    @Column(name = "users_id", nullable = false)
    private Integer usersId;

    // Token 的 SHA-256 雜湊值，不儲存原始 Token
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Token 到期時間
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 是否已使用
    @Column(name = "used", nullable = false)
    private boolean used = false;

    // 建立時間
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}