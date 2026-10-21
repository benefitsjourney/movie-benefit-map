package com.collection.benefitsjourney.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.config.JacksonConfig;
import com.collection.benefitsjourney.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 處理 RequestBody 欄位驗證錯誤 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(msg -> msg != null && !msg.isBlank())
                .distinct()
                .collect(Collectors.joining("；"));

        return buildError(
                HttpStatus.BAD_REQUEST,
                message.isBlank()
                        ? "請求內容格式不正確"
                        : message,
                request);
    }

    /** 處理 JSON 格式或 RequestBody 欄位型別錯誤 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                readableMessage(exception),
                request);
    }

    /** 處理 PathVariable 或 RequestParam 型別錯誤，包含不存在的 Enum */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                typeMismatchMessage(exception),
                request);
    }

    /** 處理不支援的 Content-Type */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "不支援的 Content-Type",
                request);
    }

    /** 處理不支援的 HTTP Method */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "不支援的 HTTP Method：" + exception.getMethod(),
                request);
    }

    /** 處理主動拋出的 HTTP 錯誤，狀態碼沿用 exception 本身 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request) {

        HttpStatus status = resolveStatus(exception.getStatusCode());

        return buildError(
                status,
                exception.getReason() != null
                        ? exception.getReason()
                        : status.getReasonPhrase(),
                request);
    }

    /** 處理不合法的請求，例如 Google ID Token 驗證失敗 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                exception.getMessage() != null
                        ? exception.getMessage()
                        : "請求參數不正確",
                request);
    }

    /**
     * 真正未預期的錯誤才回 500。
     * Spring MVC 已帶狀態碼的 ErrorResponse 沿用該狀態，避免被轉成 500。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request) {

        if (exception instanceof org.springframework.web.ErrorResponse errorResponse) {
            HttpStatus status = resolveStatus(errorResponse.getStatusCode());
            String detail = errorResponse.getBody() != null
                    ? errorResponse.getBody().getDetail()
                    : null;

            return buildError(
                    status,
                    detail != null && !detail.isBlank()
                            ? detail
                            : status.getReasonPhrase(),
                    request);
        }

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "系統發生錯誤",
                request);
    }

    private String typeMismatchMessage(MethodArgumentTypeMismatchException exception) {
        String name = exception.getName();
        Object rejected = exception.getValue();
        Class<?> requiredType = exception.getRequiredType();

        if (requiredType != null && requiredType.isEnum()) {
            return name + " 參數值無效：" + rejected;
        }

        return name + " 參數格式不正確：" + rejected;
    }

    /** 處理 JSON 格式、Enum 及日期時間錯誤 */
    private String readableMessage(HttpMessageNotReadableException exception) {
        InvalidFormatException invalidFormat = findInvalidFormat(exception);

        if (invalidFormat == null) {
            return "請求內容格式不正確";
        }

        Class<?> type = invalidFormat.getTargetType();

        // 處理 Enum 錯誤
        if (type != null && type.isEnum()) {
            String fieldName = invalidFormat.getPath()
                    .stream()
                    .map(reference -> reference == null
                            ? null
                            : reference.getPropertyName())
                    .filter(name -> name != null && !name.isBlank())
                    .reduce((first, second) -> second)
                    .orElse("欄位");

            return fieldName + " 參數值無效：" + invalidFormat.getValue();
        }

        // 處理日期時間錯誤
        if (LocalDateTime.class.equals(type)) {
            String message = invalidFormat.getOriginalMessage();

            if (message != null) {
                if (message.contains(JacksonConfig.INVALID_DATE_MESSAGE)) {
                    return JacksonConfig.INVALID_DATE_MESSAGE;
                }

                if (message.contains(JacksonConfig.INVALID_TIME_MESSAGE)) {
                    return JacksonConfig.INVALID_TIME_MESSAGE;
                }
            }

            return JacksonConfig.INVALID_DATE_TIME_MESSAGE;
        }

        return "請求內容格式不正確";
    }

    /** 日期錯誤的最底層是 DateTimeParseException，格式例外在它的外層 */
    private InvalidFormatException findInvalidFormat(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormat) {
                return invalidFormat;
            }
            current = current.getCause();
        }
        return null;
    }

    private HttpStatus resolveStatus(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /** 建立統一錯誤回傳格式 */
    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                LocalDateTime.now());

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
