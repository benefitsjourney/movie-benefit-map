package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;


@Getter 
@Setter 
public class LoginLogResponse {
    private Integer loginLogId;

    private Integer usersId;

    private String userName;

    private LocalDateTime loginTime;

    private String loginStatus;

    private String ipAddress;

    private String userAgent;
}
