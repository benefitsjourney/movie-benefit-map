package com.collection.benefitsjourney.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UsersBean {

    // 會員編號
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Integer usersId;

    // 一般登入帳號
    @Column(name = "account", unique = true)
    private String account;

    // BCrypt 密碼
    @Column(name = "password", length = 255)
    private String password;

    // 姓名 / 暱稱
    @Column(name = "name")
    private String name;

    // Email
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    // Google OAuth 唯一識別 ID
    @Column(name = "google_id", unique = true)
    private String googleId;

    // 生日
    @Column(name = "birth_date")
    private LocalDate birthDate;

    // USER / ADMIN
    @Column(name = "role", nullable = false)
    private String role;

    // ACTIVE / INACTIVE / BANNED
    @Column(name = "status")
    private String status;

    // 註冊時間
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 修改時間
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

