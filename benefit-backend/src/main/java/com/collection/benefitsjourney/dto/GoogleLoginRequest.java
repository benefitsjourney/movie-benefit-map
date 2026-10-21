package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID Token 不可為空")
    private String idToken;
    
}
