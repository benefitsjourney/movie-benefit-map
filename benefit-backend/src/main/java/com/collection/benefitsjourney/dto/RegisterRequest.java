
package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {

    // 一般登入帳號
    @NotBlank
    private String account;

    // 密碼
    @NotBlank
    private String password;

    // 姓名 / 暱稱
    @NotBlank
    private String name;

    // Email
    @NotBlank
    @Email
    private String email;

    // 生日
    @NotNull
    private LocalDate birthDate;
}

