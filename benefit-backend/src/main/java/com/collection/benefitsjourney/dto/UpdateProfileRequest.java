package com.collection.benefitsjourney.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class UpdateProfileRequest {
    
    @NotBlank(message = "姓名不能為空")
    private String name;

    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotNull(message = "生日不能為空")
    private LocalDate birthDate;
    
}
