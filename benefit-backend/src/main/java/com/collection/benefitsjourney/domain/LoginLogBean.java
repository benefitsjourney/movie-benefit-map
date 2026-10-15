package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table(name="login_log")
public class LoginLogBean {
    
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "login_log_id")
private Integer loginLogId;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private UsersBean user;

@Column(name = "login_time", nullable = false)
private LocalDateTime loginTime;

@Column(name = "login_status", nullable = false, length = 20)
private String loginStatus;

@Column(name = "ip_address", length = 45)
private String ipAddress;

@Column(name = "user_agent", length = 500)
private String userAgent;

public LoginLogBean() {
}

public Integer getLoginLogId() {
    return loginLogId;
}

public void setLoginLogId(Integer loginLogId) {
    this.loginLogId = loginLogId;
}

public UsersBean getUser() {
    return user;
}

public void setUser(UsersBean user) {
    this.user = user;
}

public LocalDateTime getLoginTime() {
    return loginTime;
}

public void setLoginTime(LocalDateTime loginTime) {
    this.loginTime = loginTime;
}

public String getLoginStatus() {
    return loginStatus;
}

public void setLoginStatus(String loginStatus) {
    this.loginStatus = loginStatus;
}

public String getIpAddress() {
    return ipAddress;
}

public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
}

public String getUserAgent() {
    return userAgent;
}

public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
}
}
