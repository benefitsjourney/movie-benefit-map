package com.collection.benefitsjourney.controller;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.UpdateUserStatusRequest;
import com.collection.benefitsjourney.dto.UserResponse;
import com.collection.benefitsjourney.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UsersService usersService;

    public AdminController(UsersService usersService) {
        this.usersService = usersService;
    }

    // 管理員修改會員狀態
    @PatchMapping("/users/{id}/status")
    public UserResponse updateUserStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest httpRequest) {

        // 取得目前登入者 ID
        Integer adminId =
                (Integer) httpRequest.getAttribute("usersId");

        // 確認目前登入者是否為 ADMIN
        usersService.checkAdmin(adminId);

        // 修改指定會員狀態
        UsersBean usersBean =
                usersService.updateAdminStatus(
                        id,
                        request.getStatus()
                );

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
}