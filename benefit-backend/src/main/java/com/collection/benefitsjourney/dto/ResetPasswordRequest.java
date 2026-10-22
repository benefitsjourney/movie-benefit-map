package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token 不可為空")
    private String token;

    @NotBlank(message = "新密碼不可為空")
    @Size(min = 8, max = 16, message = "新密碼長度須為 8 至 16 個字元")
    private String newPassword;
}