
package com.collection.benefitsjourney.controller;

import com.collection.benefitsjourney.service.UsersService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UsersService usersService;

    public UserController(UsersService usersService) {
        this.usersService = usersService;
    }

    // 取得目前登入會員資料
    @GetMapping("/me")
    public void getMyProfile() {

    }

    // 修改目前登入會員資料
    @PutMapping("/me")
    public void updateMyProfile() {

    }

    // 修改目前登入會員密碼
    @PutMapping("/me/password")
    public void changeMyPassword() {

    }

    // 查看指定會員資料
    @GetMapping("/{id}")
    public void getUserById(@PathVariable Integer id) {

    }

    // 查看指定會員發布的文章
    @GetMapping("/{id}/posts")
    public void getUserPosts(@PathVariable Integer id) {

    }

    // 停用目前登入會員帳號
    @PatchMapping("/me/status")
    public void updateMyStatus() {

    }

}

