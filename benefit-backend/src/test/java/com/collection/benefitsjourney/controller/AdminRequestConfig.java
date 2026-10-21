package com.collection.benefitsjourney.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.service.UsersService;

/**
 * 讓既有 Controller 測試帶著管理員身分。
 * JWT 過濾器在這些測試是關閉的，這裡改由 request attribute 模擬已登入的管理員。
 */
@TestConfiguration
public class AdminRequestConfig implements MockMvcBuilderCustomizer {

    public static final int ADMIN_ID = 1;

    public static void stubAdmin(UsersService usersService) {
        UsersBean admin = new UsersBean();
        admin.setUsersId(ADMIN_ID);
        admin.setRole("ADMIN");
        when(usersService.getUserById(ADMIN_ID)).thenReturn(admin);
    }

    @Override
    public void customize(org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder<?> builder) {
        builder.defaultRequest(get("/").requestAttr("usersId", ADMIN_ID));
    }
}
