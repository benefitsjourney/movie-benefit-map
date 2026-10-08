
package com.collection.benefitsjourney.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponse {

    private Integer usersId;

    private String account;

    private String name;

    private String email;

    private LocalDate birthDate;

    private String role;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
