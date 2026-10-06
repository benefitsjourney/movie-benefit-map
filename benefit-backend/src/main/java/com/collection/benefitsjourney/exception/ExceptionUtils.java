package com.collection.benefitsjourney.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ExceptionUtils {

    private ExceptionUtils() {
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
