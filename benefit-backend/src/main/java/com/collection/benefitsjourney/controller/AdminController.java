package com.collection.benefitsjourney.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.LoginLogResponse;
import com.collection.benefitsjourney.dto.UpdateUserStatusRequest;
import com.collection.benefitsjourney.dto.UserResponse;
import com.collection.benefitsjourney.repository.LoginLogRepository;
import com.collection.benefitsjourney.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

        private final UsersService usersService;
        private final LoginLogRepository loginLogRepository;

        public AdminController(UsersService usersService, LoginLogRepository loginLogRepository) {
                this.usersService = usersService;
                this.loginLogRepository = loginLogRepository;
        }

        // 管理員修改會員狀態
        @PatchMapping("/users/{id}/status")
        public UserResponse updateUserStatus(
                        @PathVariable Integer id,
                        @Valid @RequestBody UpdateUserStatusRequest request,
                        HttpServletRequest httpRequest) {

                // 取得目前登入者 ID
                Integer adminId = (Integer) httpRequest.getAttribute("usersId");

                // 確認目前登入者是否為 ADMIN
                usersService.checkAdmin(adminId);

                // 修改指定會員狀態
                UsersBean usersBean = usersService.updateAdminStatus(
                                id,
                                request.getStatus());

                // 回傳會員資料
                UserResponse response = new UserResponse();

                response.setUsersId(usersBean.getUsersId());
                response.setAccount(usersBean.getAccount());
                response.setName(usersBean.getName());
                response.setEmail(usersBean.getEmail());
                response.setBirthDate(usersBean.getBirthDate());
                response.setRole(usersBean.getRole());
                response.setStatus(usersBean.getStatus());
                response.setCreatedAt(usersBean.getCreatedAt());
                response.setUpdatedAt(usersBean.getUpdatedAt());

                return response;
        }

        // 管理員查看所有會員的登入紀錄
        @GetMapping("/login-logs")
        public ResponseEntity<?> getAllLoginLogs(
                        @RequestParam(defaultValue = "0") int page, HttpServletRequest httpRequest) {

                // 取得目前登入者 ID
                Integer adminId = (Integer) httpRequest.getAttribute("usersId");

                // 確認目前登入者是否為 ADMIN
                usersService.checkAdmin(adminId);
                PageRequest pageable = PageRequest.of(
                                Math.max(page, 0),
                                15);

                // 查詢所有登入紀錄，最新的排在前面
                Page<LoginLogResponse> response = loginLogRepository.findAll(pageable)
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

        
        // 管理員查詢會員列表
        @GetMapping("/users")
        public ResponseEntity<List<UserResponse>> getAllUsers(
                @RequestParam(required = false) String status,
                HttpServletRequest httpRequest) {

                // 確認目前登入者是管理員
                Integer adminId = (Integer) httpRequest.getAttribute("usersId");
                usersService.checkAdmin(adminId);

                // 限制可查詢的會員狀態
                if (status != null && !status.isBlank()
                        && !List.of("ACTIVE", "INACTIVE", "BANNED", "DELETED")
                                .contains(status)) {
                throw new IllegalArgumentException("無效的會員狀態");
                }

                // 預設排除已刪除會員；指定狀態時只查詢該狀態
                return ResponseEntity.ok(
                        usersService.findAll().stream()
                                .filter(user -> {
                                if (status != null && !status.isBlank()) {
                                        return status.equals(user.getStatus());
                                }
                                return !"DELETED".equals(user.getStatus());
                                })
                                .map(user -> {
                                UserResponse response = new UserResponse();
                                response.setUsersId(user.getUsersId());
                                response.setAccount(user.getAccount());
                                response.setName(user.getName());
                                response.setEmail(user.getEmail());
                                response.setBirthDate(user.getBirthDate());
                                response.setRole(user.getRole());
                                response.setStatus(user.getStatus());
                                response.setCreatedAt(user.getCreatedAt());
                                response.setUpdatedAt(user.getUpdatedAt());
                                return response;
                                })
                                .collect(Collectors.toList())
                );
        }


        @GetMapping("/login-logs/{userId}")
        public ResponseEntity<?> getUserLoginLogs(
                        @PathVariable Integer userId,
                        @RequestParam(defaultValue = "0") int page,
                        HttpServletRequest httpRequest) {

                // 取得目前登入者 ID
                Integer adminId = (Integer) httpRequest.getAttribute("usersId");

                // 確認目前登入者是否為 ADMIN
                usersService.checkAdmin(adminId);

                // 查詢指定會員的登入紀錄，最新的排在前面
                PageRequest pageable = PageRequest.of(
                        Math.max(page, 0),
                        15);

                Page<LoginLogResponse> response = loginLogRepository
                                .findByUser_UsersIdOrderByLoginTimeDesc(userId, pageable)
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