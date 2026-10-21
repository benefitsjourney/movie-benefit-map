
package com.collection.benefitsjourney.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    // 一般登入帳號
    @NotBlank
    private String account;

    // 密碼
    @NotBlank
    @Size(min = 8, max = 16, message = "密碼長度須為 8～16 碼")
    private String password;

    // 姓名 / 暱稱（選填，未填時使用登入帳號）
    private String name;

    // Email
    @NotBlank
    @Email
    private String email;

    // 生日(選填)
    private LocalDate birthDate;
}

