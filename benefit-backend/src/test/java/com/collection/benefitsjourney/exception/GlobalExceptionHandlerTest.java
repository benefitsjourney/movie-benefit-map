
package com.collection.benefitsjourney.exception;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.config.JacksonConfig;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        JacksonConfig.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /** RequestBody 欄位驗證 */
    @Nested
    @DisplayName("RequestBody 驗證錯誤")
    class ValidationTests {

        @Test
        @DisplayName("必填欄位空白應回傳 400")
        void blankField() throws Exception {
            ResultActions result = postJson(
                    "/test/validation",
                    """
                            {"name": " "}
                            """);

            assertError(result, 400, "Bad Request",
                    "名稱不能為空", "/test/validation");
        }

        @Test
        @DisplayName("多個欄位錯誤應合併訊息")
        void multipleErrors() throws Exception {
            postJson("/test/validation/multiple",
                    """
                            {"name": "", "code": ""}
                            """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value(containsString("名稱不能為空")))
                    .andExpect(jsonPath("$.message")
                            .value(containsString("代碼不能為空")));
        }

        @Test
        @DisplayName("沒有有效驗證訊息應使用預設訊息")
        void blankMessage() throws Exception {
            ResultActions result = postJson(
                    "/test/validation/blank-message",
                    """
                            {"value": ""}
                            """);

            assertError(result, 400, "Bad Request",
                    "請求內容格式不正確",
                    "/test/validation/blank-message");
        }
    }

    /** JSON 讀取及資料型別錯誤 */
    @Nested
    @DisplayName("RequestBody 讀取錯誤")
    class NotReadableTests {

        @Test
        @DisplayName("JSON 語法錯誤應回傳 400")
        void invalidJson() throws Exception {
            ResultActions result = postJson(
                    "/test/body/enum",
                    """
                            {"mode": "ACTIVE"
                            """);

            assertError(result, 400, "Bad Request",
                    "請求內容格式不正確",
                    "/test/body/enum");
        }

        @Test
        @DisplayName("Enum 不存在應顯示欄位與錯誤值")
        void invalidEnum() throws Exception {
            ResultActions result = postJson(
                    "/test/body/enum",
                    """
                            {"mode": "UNKNOWN"}
                            """);

            assertError(result, 400, "Bad Request",
                    "mode 參數值無效：UNKNOWN",
                    "/test/body/enum");
        }

        @Test
        @DisplayName("一般欄位型別錯誤應回傳 400")
        void invalidNumber() throws Exception {
            ResultActions result = postJson(
                    "/test/body/number",
                    """
                            {"count": "hello"}
                            """);

            assertError(result, 400, "Bad Request",
                    "請求內容格式不正確",
                    "/test/body/number");
        }

        @Test
        @DisplayName("缺少 RequestBody 應回傳 400")
        void missingBody() throws Exception {
            ResultActions result = mockMvc.perform(
                    post("/test/body/enum")
                            .contentType(MediaType.APPLICATION_JSON));

            assertError(result, 400, "Bad Request",
                    "請求內容格式不正確",
                    "/test/body/enum");
        }
    }

    /** 日期時間格式及有效性驗證 */
    @Nested
    @DisplayName("LocalDateTime 日期時間驗證")
    class DateTimeTests {

        @ParameterizedTest(name = "格式錯誤：{0}")
        @ValueSource(strings = {
                "不是日期",
                "2026/10/08 12:30:00",
                "2026-10-08",
                "2026-10-08T12:30:00",
                "2026-10-08 12:30",
                "2026-10-08 12:30:00.123",
                "2026-1-8 12:30:00",
                "2026-10-08 1:30:00",
                "2026-10-08 12:30:00Z",
                " 2026-10-08 12:30:00 "
        })
        @DisplayName("日期時間格式錯誤應回傳 400")
        void invalidFormat(String value) throws Exception {
            assertDateError(
                    value,
                    JacksonConfig.INVALID_DATE_TIME_MESSAGE);
        }

        @ParameterizedTest(name = "日期不存在：{0}")
        @ValueSource(strings = {
                "2026-02-29 12:30:00",
                "2026-02-30 12:30:00",
                "2026-04-31 12:30:00",
                "2026-06-31 12:30:00",
                "2026-09-31 12:30:00",
                "2026-11-31 12:30:00",
                "2026-00-08 12:30:00",
                "2026-13-08 12:30:00",
                "2026-10-00 12:30:00",
                "2026-10-32 12:30:00"
        })
        @DisplayName("不存在的日期應回傳 400")
        void invalidDate(String value) throws Exception {
            assertDateError(
                    value,
                    JacksonConfig.INVALID_DATE_MESSAGE);
        }

        @ParameterizedTest(name = "時間不存在：{0}")
        @ValueSource(strings = {
                "2026-10-08 24:00:00",
                "2026-10-08 25:30:00",
                "2026-10-08 99:30:00",
                "2026-10-08 12:60:00",
                "2026-10-08 12:61:00",
                "2026-10-08 12:30:60",
                "2026-10-08 12:30:99"
        })
        @DisplayName("不存在的時間應回傳 400")
        void invalidTime(String value) throws Exception {
            assertDateError(
                    value,
                    JacksonConfig.INVALID_TIME_MESSAGE);
        }

        @ParameterizedTest(name = "有效日期時間：{0}")
        @ValueSource(strings = {
                "2026-10-08 12:30:00",
                "2026-01-01 00:00:00",
                "2026-12-31 23:59:59",
                "2024-02-29 12:30:00",
                "2000-02-29 12:30:00",
                "2026-02-28 12:30:00"
        })
        @DisplayName("有效日期時間應成功解析")
        void validDateTime(String value) throws Exception {
            postJson("/test/body/date",
                    dateJson(value))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("日期時間為 null 應允許解析")
        void nullDateTime() throws Exception {
            postJson("/test/body/date",
                    """
                            {"time": null}
                            """)
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("日期時間不是字串應回傳 400")
        void dateTimeWrongType() throws Exception {
            ResultActions result = postJson(
                    "/test/body/date",
                    """
                            {"time": 12345}
                            """);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.path")
                            .value("/test/body/date"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("日期錯誤應維持統一 JSON 錯誤格式")
        void invalidDateResponseStructure() throws Exception {
            ResultActions result = postJson(
                    "/test/body/date",
                    dateJson("2026-02-30 12:30:00"));

            assertError(result, 400, "Bad Request",
                    JacksonConfig.INVALID_DATE_MESSAGE,
                    "/test/body/date");
        }
    }

    /** RequestParam 型別錯誤 */
    @Nested
    @DisplayName("RequestParam 型別錯誤")
    class TypeMismatchTests {

        @Test
        @DisplayName("Enum 參數不存在應回傳 400")
        void invalidEnumParameter() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/param/enum")
                            .param("mode", "UNKNOWN"));

            assertError(result, 400, "Bad Request",
                    "mode 參數值無效：UNKNOWN",
                    "/test/param/enum");
        }

        @Test
        @DisplayName("數字參數格式錯誤應回傳 400")
        void invalidNumberParameter() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/param/number")
                            .param("page", "abc"));

            assertError(result, 400, "Bad Request",
                    "page 參數格式不正確：abc",
                    "/test/param/number");
        }
    }

    /** HTTP 請求錯誤 */
    @Nested
    @DisplayName("HTTP 請求錯誤")
    class HttpTests {

        @Test
        @DisplayName("不支援 Content-Type 應回傳 415")
        void unsupportedMediaType() throws Exception {
            ResultActions result = mockMvc.perform(
                    post("/test/validation")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("test"));

            assertError(result, 415, "Unsupported Media Type",
                    "不支援的 Content-Type",
                    "/test/validation");
        }

        @Test
        @DisplayName("不支援 HTTP Method 應回傳 405")
        void unsupportedMethod() throws Exception {
            ResultActions result = mockMvc.perform(
                    put("/test/method"));

            assertError(result, 405, "Method Not Allowed",
                    "不支援的 HTTP Method：PUT",
                    "/test/method");
        }
    }

    /** 主動拋出的 HTTP 錯誤 */
    @Nested
    @DisplayName("ResponseStatusException")
    class ResponseStatusTests {

        @Test
        @DisplayName("應保留指定狀態碼與 reason")
        void withReason() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/response-status"));

            assertError(result, 404, "Not Found",
                    "找不到測試資料",
                    "/test/response-status");
        }

        @Test
        @DisplayName("沒有 reason 的 403 應使用統一權限訊息")
        void withoutReason() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/response-status/no-reason"));

            assertError(result, 403, "Forbidden",
                    ExceptionUtils.FORBIDDEN_MESSAGE,
                    "/test/response-status/no-reason");
        }
    }

    /** Spring ErrorResponse 錯誤 */
    @Nested
    @DisplayName("Spring ErrorResponse")
    class SpringErrorTests {

        @Test
        @DisplayName("應保留 HTTP 狀態碼與 detail")
        void withDetail() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/error-response"));

            assertError(result, 409, "Conflict",
                    "資料發生衝突",
                    "/test/error-response");
        }

        @Test
        @DisplayName("沒有 detail 應使用預設訊息")
        void withoutDetail() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/error-response/no-detail"));

            assertError(result, 410, "Gone",
                    "Gone",
                    "/test/error-response/no-detail");
        }

        @Test
        @DisplayName("無法解析的 HTTP 狀態碼應降級為 500")
        void unknownStatus() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/error-response/unknown-status"));

            assertError(result, 500, "Internal Server Error",
                    "Internal Server Error",
                    "/test/error-response/unknown-status");
        }
    }

    /** 未預期錯誤 */
    @Nested
    @DisplayName("未預期 Exception")
    class UnexpectedTests {

        @Test
        @DisplayName("未預期錯誤應回傳 500")
        void unexpectedException() throws Exception {
            ResultActions result = mockMvc.perform(
                    get("/test/unexpected"));

            assertError(result, 500, "Internal Server Error",
                    "系統發生錯誤",
                    "/test/unexpected");
        }

        @Test
        @DisplayName("不應洩漏內部 Exception 訊息")
        void hideInternalMessage() throws Exception {
            mockMvc.perform(get("/test/unexpected"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message")
                            .value("系統發生錯誤"));
        }
    }

    /** 發送 JSON POST 請求 */
    private ResultActions postJson(
            String path,
            String json) throws Exception {

        return mockMvc.perform(
                post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    /** 建立日期時間測試 JSON */
    private String dateJson(String value) {
        return "{\"time\":\"" + value + "\"}";
    }

    /** 驗證日期時間錯誤 */
    private void assertDateError(
            String value,
            String message) throws Exception {

        ResultActions result = postJson(
                "/test/body/date",
                dateJson(value));

        assertError(result, 400, "Bad Request",
                message, "/test/body/date");
    }

    /** 驗證統一錯誤回傳格式 */
    private void assertError(
            ResultActions result,
            int expectedStatus,
            String expectedError,
            String expectedMessage,
            String expectedPath) throws Exception {

        result
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status")
                        .value(expectedStatus))
                .andExpect(jsonPath("$.error")
                        .value(expectedError))
                .andExpect(jsonPath("$.message")
                        .value(expectedMessage))
                .andExpect(jsonPath("$.path")
                        .value(expectedPath))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /** 測試專用 Controller */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping(value = "/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validation(
                @Valid @RequestBody ValidationRequest request) {
        }

        @PostMapping(value = "/validation/multiple", consumes = MediaType.APPLICATION_JSON_VALUE)
        void multipleValidation(
                @Valid @RequestBody MultipleValidationRequest request) {
        }

        @PostMapping(value = "/validation/blank-message", consumes = MediaType.APPLICATION_JSON_VALUE)
        void blankValidation(
                @Valid @RequestBody BlankValidationRequest request) {
        }

        @PostMapping("/body/enum")
        void enumBody(@RequestBody EnumRequest request) {
        }

        @PostMapping("/body/date")
        void dateBody(@RequestBody DateRequest request) {
        }

        @PostMapping("/body/number")
        void numberBody(@RequestBody NumberRequest request) {
        }

        @GetMapping("/param/enum")
        void enumParameter(@RequestParam TestEnum mode) {
        }

        @GetMapping("/param/number")
        void numberParameter(@RequestParam Integer page) {
        }

        @GetMapping("/method")
        void method() {
        }

        @GetMapping("/response-status")
        void responseStatus() {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "找不到測試資料");
        }

        @GetMapping("/response-status/no-reason")
        void responseStatusWithoutReason() {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN);
        }

        @GetMapping("/error-response")
        void errorResponse() {
            ErrorResponseException exception = new ErrorResponseException(HttpStatus.CONFLICT);

            exception.setDetail("資料發生衝突");
            throw exception;
        }

        @GetMapping("/error-response/no-detail")
        void errorResponseWithoutDetail() {
            throw new ErrorResponseException(HttpStatus.GONE);
        }

        @GetMapping("/error-response/unknown-status")
        void unknownStatus() {
            throw new ErrorResponseException(
                    HttpStatusCode.valueOf(499));
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException(
                    "這段內部錯誤不應該傳給前端");
        }
    }

    /** 測試專用 DTO */
    record ValidationRequest(
            @NotBlank(message = "名稱不能為空") String name) {
    }

    record MultipleValidationRequest(
            @NotBlank(message = "名稱不能為空") String name,

            @NotBlank(message = "代碼不能為空") String code) {
    }

    record BlankValidationRequest(
            @NotBlank(message = " ") String value) {
    }

    record EnumRequest(TestEnum mode) {
    }

    record DateRequest(LocalDateTime time) {
    }

    record NumberRequest(Integer count) {
    }

    enum TestEnum {
        ACTIVE,
        INACTIVE
    }
}
