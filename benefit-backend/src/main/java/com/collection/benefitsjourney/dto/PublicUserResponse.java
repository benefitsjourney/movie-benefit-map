package com.collection.benefitsjourney.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PublicUserResponse {

    private Integer usersId;

    private String account;

    private String name;

    private String status;

    private LocalDateTime createdAt;
}
