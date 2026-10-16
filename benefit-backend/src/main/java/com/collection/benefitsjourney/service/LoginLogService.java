package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.LoginLogBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.LoginLogRepository;

@Service 
public class LoginLogService {
    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    // 儲存登入紀錄
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLoginLog(
            UsersBean user,
            String loginStatus,
            String ipAddress,
            String userAgent) {

        LoginLogBean loginLog = new LoginLogBean();

        loginLog.setUser(user);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setLoginStatus(loginStatus);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);

        loginLogRepository.save(loginLog);
    }
}
