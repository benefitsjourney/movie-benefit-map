package com.collection.benefitsjourney.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;

public final class ExceptionUtils {

    /** 非管理員呼叫受保護 API 時的統一 403 訊息 */
    public static final String FORBIDDEN_MESSAGE = "您沒有權限執行此操作";

    private ExceptionUtils() {
    }

    /**
     * 確認目前登入者是管理員。
     * 未登入或角色不是 ADMIN 時丟出 403，訊息由 {@link #FORBIDDEN_MESSAGE} 統一。
     */
    public static void requireAdmin(HttpServletRequest request, UsersService usersService) {
        Integer usersId = (Integer) request.getAttribute("usersId");
        if (usersId == null) {
            throw forbidden();
        }

        UsersBean user = usersService.getUserById(usersId);
        if (!"ADMIN".equals(user.getRole())) {
            throw forbidden();
        }
    }

    /** 建立 403 Forbidden 錯誤，使用統一權限訊息 */
    public static ResponseStatusException forbidden() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                FORBIDDEN_MESSAGE);
    }

    /** 建立 400 Bad Request 錯誤 */
    public static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message);
    }

    /** 建立 404 Not Found 錯誤 */
    public static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message);
    }

    /** 建立 409 Conflict 錯誤 */
    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message);
    }
}
