package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    // 登入帳號
    @NotBlank
    private String account;

    // 登入密碼
    @NotBlank
    private String password;
}
