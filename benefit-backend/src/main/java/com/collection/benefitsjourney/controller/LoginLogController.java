package com.collection.benefitsjourney.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.LoginLogResponse;
import com.collection.benefitsjourney.repository.LoginLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/login-logs")
public class LoginLogController {
    private final LoginLogRepository loginLogRepository;

    public LoginLogController(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    // 查看自己的登入紀錄
    @GetMapping("/my")
    public ResponseEntity<Page<LoginLogResponse>> getMyLoginLogs(
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest httpRequest) {

        // 從 JWT Filter 取得目前登入會員的 ID
        Integer usersId = (Integer) httpRequest.getAttribute("usersId");

        // 查詢登入紀錄，最新的排在前面
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            15);

        // 將 Entity 轉換成 Response DTO
        Page<LoginLogResponse> response = loginLogRepository
                .findByUser_UsersIdOrderByLoginTimeDesc(usersId, pageable)
                .map(log -> {
                    LoginLogResponse item = new LoginLogResponse();
                    item.setLoginLogId(log.getLoginLogId());
                    item.setUsersId(log.getUser() != null ? log.getUser().getUsersId() : null);
                    item.setLoginTime(log.getLoginTime());
                    item.setLoginStatus(log.getLoginStatus());
                    item.setIpAddress(log.getIpAddress());
                    item.setUserAgent(log.getUserAgent());
                    return item;
                });

        return ResponseEntity.ok(response);
    }
    
}
