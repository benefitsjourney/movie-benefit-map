
package com.collection.benefitsjourney.controller;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.ChangePasswordRequest;
import com.collection.benefitsjourney.dto.UpdateProfileRequest;
import com.collection.benefitsjourney.dto.UpdateUserStatusRequest;
import com.collection.benefitsjourney.dto.UserResponse;
import com.collection.benefitsjourney.dto.PublicUserResponse;
import com.collection.benefitsjourney.service.UsersService;
import com.collection.benefitsjourney.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

        private final UsersService usersService;
        private final AuthService authService;

        public UserController(UsersService usersService, AuthService authService) {
                this.usersService = usersService;
                this.authService = authService;
        }

        // 取得目前登入會員資料
        @GetMapping("/me")
        public UserResponse getCurrentUser(HttpServletRequest request) {

                Integer usersId = (Integer) request.getAttribute("usersId");

                UsersBean usersBean = usersService.getUserById(usersId);

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

        // 修改目前登入會員資料 NAME, EMAIL, BIRTH_DATE
        @PutMapping("/me")
        public UserResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request,
                        HttpServletRequest httpRequest) {
                Integer usersId = (Integer) httpRequest.getAttribute("usersId");

                UsersBean usersBean = usersService.updateProfile(usersId, request);

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

        // 修改目前登入會員密碼
        @PutMapping("/me/password")
        public void changeMyPassword(@Valid @RequestBody ChangePasswordRequest request,
                        HttpServletRequest httpRequest) {
                Integer usersId = (Integer) httpRequest.getAttribute("usersId");

                authService.changePassword(usersId, request);
        }

        // 查看指定會員資料
        @GetMapping("/{id}")
        public PublicUserResponse getUserById(@PathVariable Integer id) {
                UsersBean usersBean = usersService.getUserById(id);

                // INACTIVE / BANNED 的會員不公開個人頁面
                if ("INACTIVE".equals(usersBean.getStatus()) || "BANNED".equals(usersBean.getStatus())) {
                        throw new IllegalArgumentException(
                                        "找不到此會員");
                }

                PublicUserResponse response = new PublicUserResponse();

                response.setUsersId(usersBean.getUsersId());
                response.setAccount(usersBean.getAccount());
                response.setName(usersBean.getName());
                response.setStatus(usersBean.getStatus());
                response.setCreatedAt(usersBean.getCreatedAt());
                return response;
        }

        // 查看指定會員發布的文章
        @GetMapping("/{id}/posts")
        public void getUserPosts(@PathVariable Integer id) {

        }

        // 停用目前登入會員帳號
        @PatchMapping("/me/status")
        public UserResponse updateMyStatus(
                        @Valid @RequestBody UpdateUserStatusRequest request,
                        HttpServletRequest httpRequest) {

                Integer usersId = (Integer) httpRequest.getAttribute("usersId");

                UsersBean usersBean = usersService.updateStatus(
                                usersId,
                                request.getStatus());

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
