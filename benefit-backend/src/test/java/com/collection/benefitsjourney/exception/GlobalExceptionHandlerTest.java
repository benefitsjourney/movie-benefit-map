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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * =========================================================
     * MethodArgumentNotValidException
     * =========================================================
     */

    @Nested
    @DisplayName("RequestBody 驗證錯誤")
    class ValidationTests {

        @Test
        @DisplayName("必填欄位空白時應回傳 400 與驗證訊息")
        void validation_BlankField_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": " "
                                    }
                                    """));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "名稱不能為空",
                    "/test/validation");
        }

        @Test
        @DisplayName("多個欄位驗證失敗時應合併錯誤訊息")
        void validation_MultipleErrors_ShouldReturnMessages() throws Exception {

            mockMvc.perform(
                    post("/test/validation/multiple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "",
                                        "code": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(
                            containsString("名稱不能為空")))
                    .andExpect(jsonPath("$.message").value(
                            containsString("代碼不能為空")))
                    .andExpect(jsonPath("$.path").value(
                            "/test/validation/multiple"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("驗證錯誤沒有有效訊息時應使用預設訊息")
        void validation_NoMessage_ShouldUseDefaultMessage() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/validation/blank-message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "value": ""
                                    }
                                    """));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "請求內容格式不正確",
                    "/test/validation/blank-message");
        }
    }

    /**
     * =========================================================
     * HttpMessageNotReadableException
     * =========================================================
     */

    @Nested
    @DisplayName("RequestBody 讀取錯誤")
    class NotReadableTests {

        @Test
        @DisplayName("JSON 格式錯誤時應回傳 400")
        void invalidJson_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/body/enum")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "mode": "ACTIVE"
                                    """));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "請求內容格式不正確",
                    "/test/body/enum");
        }

        @Test
        @DisplayName("RequestBody Enum 不存在時應顯示欄位名稱與錯誤值")
        void invalidEnum_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/body/enum")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "mode": "UNKNOWN"
                                    }
                                    """));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "mode 參數值無效：UNKNOWN",
                    "/test/body/enum");
        }

        @Test
        @DisplayName("LocalDateTime 格式錯誤時應顯示日期格式訊息")
        void invalidDateTime_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/body/date")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "time": "不是日期"
                                    }
                                    """));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    JacksonConfig.INVALID_DATE_TIME_MESSAGE,
                    "/test/body/date");
        }

        @Test
        @DisplayName("一般欄位型別錯誤時應回傳通用格式錯誤")
        void invalidFieldType_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/body/number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "count": "hello"
                                    }
                                    """));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "請求內容格式不正確",
                    "/test/body/number");
        }

        @Test
        @DisplayName("缺少 RequestBody 時應回傳 400")
        void missingBody_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/body/enum")
                            .contentType(MediaType.APPLICATION_JSON));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "請求內容格式不正確",
                    "/test/body/enum");
        }
    }

    /**
     * =========================================================
     * MethodArgumentTypeMismatchException
     * =========================================================
     */

    @Nested
    @DisplayName("RequestParam 型別錯誤")
    class TypeMismatchTests {

        @Test
        @DisplayName("Enum RequestParam 不存在時應顯示參數名稱與錯誤值")
        void invalidEnumParameter_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/param/enum")
                            .param("mode", "UNKNOWN"));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "mode 參數值無效：UNKNOWN",
                    "/test/param/enum");
        }

        @Test
        @DisplayName("數字 RequestParam 格式錯誤時應顯示參數格式不正確")
        void invalidNumberParameter_ShouldReturn400() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/param/number")
                            .param("page", "abc"));

            assertError(
                    result,
                    400,
                    "Bad Request",
                    "page 參數格式不正確：abc",
                    "/test/param/number");
        }
    }

    /**
     * =========================================================
     * HttpMediaTypeNotSupportedException
     * =========================================================
     */

    @Nested
    @DisplayName("Content-Type 錯誤")
    class MediaTypeTests {

        @Test
        @DisplayName("不支援的 Content-Type 應回傳 415")
        void unsupportedMediaType_ShouldReturn415() throws Exception {

            ResultActions result = mockMvc.perform(
                    post("/test/validation")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("""
                                    {
                                        "name": "測試"
                                    }
                                    """));

            assertError(
                    result,
                    415,
                    "Unsupported Media Type",
                    "不支援的 Content-Type",
                    "/test/validation");
        }
    }

    /**
     * =========================================================
     * HttpRequestMethodNotSupportedException
     * =========================================================
     */

    @Nested
    @DisplayName("HTTP Method 錯誤")
    class MethodNotSupportedTests {

        @Test
        @DisplayName("不支援的 PUT Method 應回傳 405")
        void unsupportedMethod_ShouldReturn405() throws Exception {

            ResultActions result = mockMvc.perform(
                    put("/test/method"));

            assertError(
                    result,
                    405,
                    "Method Not Allowed",
                    "不支援的 HTTP Method：PUT",
                    "/test/method");
        }
    }

    /**
     * =========================================================
     * ResponseStatusException
     * =========================================================
     */

    @Nested
    @DisplayName("ResponseStatusException")
    class ResponseStatusTests {

        @Test
        @DisplayName("ResponseStatusException 應沿用指定狀態碼與 reason")
        void responseStatus_WithReason_ShouldUseStatusAndReason() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/response-status"));

            assertError(
                    result,
                    404,
                    "Not Found",
                    "找不到測試資料",
                    "/test/response-status");
        }

        @Test
        @DisplayName("沒有 reason 時應使用 HTTP 狀態預設訊息")
        void responseStatus_NoReason_ShouldUseReasonPhrase() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/response-status/no-reason"));

            assertError(
                    result,
                    403,
                    "Forbidden",
                    "Forbidden",
                    "/test/response-status/no-reason");
        }
    }

    /**
     * =========================================================
     * Spring ErrorResponse
     * =========================================================
     */

    @Nested
    @DisplayName("Spring ErrorResponse")
    class SpringErrorResponseTests {

        @Test
        @DisplayName("Spring ErrorResponse 應保留原本的 HTTP 狀態碼")
        void errorResponse_WithDetail_ShouldKeepStatus() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/error-response"));

            assertError(
                    result,
                    409,
                    "Conflict",
                    "資料發生衝突",
                    "/test/error-response");
        }

        @Test
        @DisplayName("Spring ErrorResponse 沒有 detail 時應使用狀態預設訊息")
        void errorResponse_NoDetail_ShouldUseReasonPhrase() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/error-response/no-detail"));

            assertError(
                    result,
                    410,
                    "Gone",
                    "Gone",
                    "/test/error-response/no-detail");
        }

        @Test
        @DisplayName("未知 HTTP 狀態碼應降級成 500")
        void errorResponse_UnknownStatus_ShouldReturn500() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/error-response/unknown-status"));

            assertError(
                    result,
                    500,
                    "Internal Server Error",
                    "Internal Server Error",
                    "/test/error-response/unknown-status");
        }
    }

    /**
     * =========================================================
     * Unexpected Exception
     * =========================================================
     */

    @Nested
    @DisplayName("未預期錯誤")
    class UnexpectedExceptionTests {

        @Test
        @DisplayName("一般未預期 Exception 應回傳 500")
        void unexpectedException_ShouldReturn500() throws Exception {

            ResultActions result = mockMvc.perform(
                    get("/test/unexpected"));

            assertError(
                    result,
                    500,
                    "Internal Server Error",
                    "系統發生錯誤",
                    "/test/unexpected");
        }

        @Test
        @DisplayName("500 不應將原始 Exception 訊息回傳給前端")
        void unexpectedException_ShouldHideInternalMessage() throws Exception {

            mockMvc.perform(
                    get("/test/unexpected"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("系統發生錯誤"));
        }
    }

    /**
     * 驗證統一錯誤格式。
     */
    private void assertError(
            ResultActions result,
            int expectedStatus,
            String expectedError,
            String expectedMessage,
            String expectedPath) throws Exception {

        result
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.error").value(expectedError))
                .andExpect(jsonPath("$.message").value(expectedMessage))
                .andExpect(jsonPath("$.path").value(expectedPath))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * =========================================================
     * 測試專用 Controller
     * =========================================================
     *
     * 不包含任何實際業務邏輯，
     * 只用來觸發 GlobalExceptionHandler 的各種 Exception。
     */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        /**
         * RequestBody 驗證錯誤
         */
        @PostMapping(value = "/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validation(
                @Valid @RequestBody ValidationRequest request) {
        }

        /**
         * 多個 RequestBody 驗證錯誤
         */
        @PostMapping(value = "/validation/multiple", consumes = MediaType.APPLICATION_JSON_VALUE)
        void multipleValidation(
                @Valid @RequestBody MultipleValidationRequest request) {
        }

        /**
         * 驗證訊息為空白
         */
        @PostMapping(value = "/validation/blank-message", consumes = MediaType.APPLICATION_JSON_VALUE)
        void blankValidationMessage(
                @Valid @RequestBody BlankValidationRequest request) {
        }

        /**
         * Enum RequestBody
         */
        @PostMapping(value = "/body/enum", consumes = MediaType.APPLICATION_JSON_VALUE)
        void enumBody(
                @RequestBody EnumRequest request) {
        }

        /**
         * LocalDateTime RequestBody
         */
        @PostMapping(value = "/body/date", consumes = MediaType.APPLICATION_JSON_VALUE)
        void dateBody(
                @RequestBody DateRequest request) {
        }

        /**
         * Integer RequestBody
         */
        @PostMapping(value = "/body/number", consumes = MediaType.APPLICATION_JSON_VALUE)
        void numberBody(
                @RequestBody NumberRequest request) {
        }

        /**
         * Enum RequestParam
         */
        @GetMapping("/param/enum")
        void enumParameter(
                @RequestParam TestEnum mode) {
        }

        /**
         * Integer RequestParam
         */
        @GetMapping("/param/number")
        void numberParameter(
                @RequestParam Integer page) {
        }

        /**
         * 正常只支援 GET，
         * 測試時故意使用 PUT 觸發 405。
         */
        @GetMapping("/method")
        void method() {
        }

        /**
         * 主動拋出帶 reason 的 HTTP 404。
         */
        @GetMapping("/response-status")
        void responseStatus() {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "找不到測試資料");
        }

        /**
         * 主動拋出沒有 reason 的 HTTP 403。
         */
        @GetMapping("/response-status/no-reason")
        void responseStatusWithoutReason() {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN);
        }

        /**
         * Spring ErrorResponse，
         * 用來確認 handleException 不會全部轉成 500。
         */
        @GetMapping("/error-response")
        void errorResponse() {
            ErrorResponseException exception = new ErrorResponseException(HttpStatus.CONFLICT);

            exception.setDetail("資料發生衝突");

            throw exception;
        }

        /**
         * Spring ErrorResponse 沒有 detail。
         */
        @GetMapping("/error-response/no-detail")
        void errorResponseWithoutDetail() {
            throw new ErrorResponseException(
                    HttpStatus.GONE);
        }

        /**
         * Spring 無法解析成 HttpStatus 的狀態碼，
         * 應由 resolveStatus 降級成 500。
         */
        @GetMapping("/error-response/unknown-status")
        void unknownStatus() {
            throw new ErrorResponseException(
                    HttpStatusCode.valueOf(499));
        }

        /**
         * 真正未預期的程式錯誤。
         */
        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException(
                    "這段內部錯誤不應該傳給前端");
        }
    }

    /**
     * =========================================================
     * 測試專用 Request
     * =========================================================
     */

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

    record EnumRequest(
            TestEnum mode) {
    }

    record DateRequest(
            LocalDateTime time) {
    }

    record NumberRequest(
            Integer count) {
    }

    enum TestEnum {
        ACTIVE,
        INACTIVE
    }
}