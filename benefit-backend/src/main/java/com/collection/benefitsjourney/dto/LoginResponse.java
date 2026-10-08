package com.collection.benefitsjourney.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private String token;

    private String tokenType;

    private UserResponse user;
}