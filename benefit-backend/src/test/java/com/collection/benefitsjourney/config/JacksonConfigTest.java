
package com.collection.benefitsjourney.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("JacksonConfig 測試")
class JacksonConfigTest {

    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        JsonMapper.Builder builder = JsonMapper.builder();

        new JacksonConfig()
                .localDateTimeCustomizer()
                .customize(builder);

        mapper = builder.build();
    }

    /** LocalDateTime 序列化 */
    @Nested
    @DisplayName("LocalDateTime 序列化")
    class SerializationTests {

        @Test
        @DisplayName("正常日期時間應輸出指定格式")
        void validDateTime() {
            LocalDateTime date = LocalDateTime.of(
                    2026, 10, 8, 14, 30, 45);

            assertEquals(
                    "\"2026-10-08 14:30:45\"",
                    mapper.writeValueAsString(date));
        }

        @Test
        @DisplayName("個位數日期時間應自動補零")
        void singleDigitValues() {
            LocalDateTime date = LocalDateTime.of(
                    2026, 1, 2, 3, 4, 5);

            assertEquals(
                    "\"2026-01-02 03:04:05\"",
                    mapper.writeValueAsString(date));
        }

        @Test
        @DisplayName("毫秒及奈秒不應出現在輸出結果")
        void nanoseconds() {
            LocalDateTime date = LocalDateTime.of(
                    2026, 10, 8, 14, 30, 45, 123456789);

            assertEquals(
                    "\"2026-10-08 14:30:45\"",
                    mapper.writeValueAsString(date));
        }

        @Test
        @DisplayName("午夜時間應正確輸出")
        void midnight() {
            LocalDateTime date = LocalDateTime.of(
                    2026, 10, 8, 0, 0, 0);

            assertEquals(
                    "\"2026-10-08 00:00:00\"",
                    mapper.writeValueAsString(date));
        }

        @Test
        @DisplayName("當天最後一秒應正確輸出")
        void endOfDay() {
            LocalDateTime date = LocalDateTime.of(
                    2026, 10, 8, 23, 59, 59);

            assertEquals(
                    "\"2026-10-08 23:59:59\"",
                    mapper.writeValueAsString(date));
        }

        @Test
        @DisplayName("閏年二月二十九日應正確輸出")
        void leapYear() {
            LocalDateTime date = LocalDateTime.of(
                    2024, 2, 29, 12, 30, 0);

            assertEquals(
                    "\"2024-02-29 12:30:00\"",
                    mapper.writeValueAsString(date));
        }

        @Test
        @DisplayName("null 應輸出 JSON null")
        void nullValue() {
            assertEquals(
                    "null",
                    mapper.writeValueAsString(null));
        }
    }

    /** LocalDateTime 反序列化 */
    @Nested
    @DisplayName("LocalDateTime 反序列化")
    class DeserializationTests {

        @ParameterizedTest(name = "有效日期時間：{0}")
        @ValueSource(strings = {
                "2026-10-08 14:30:45",
                "2026-01-01 00:00:00",
                "2026-12-31 23:59:59",
                "2024-02-29 12:30:00",
                "2000-02-29 12:30:00",
                "2026-02-28 12:30:00",
                "2026-04-30 12:30:00"
        })
        @DisplayName("有效日期時間應正確解析")
        void validDateTime(String value) {
            LocalDateTime result = mapper.readValue(
                    json(value),
                    LocalDateTime.class);

            assertEquals(
                    LocalDateTime.parse(
                            value.replace(" ", "T")),
                    result);
        }

        @Test
        @DisplayName("JSON null 應回傳 null")
        void nullValue() {
            LocalDateTime result = mapper.readValue(
                    "null",
                    LocalDateTime.class);

            assertNull(result);
        }
    }

    /** 日期時間格式錯誤 */
    @Nested
    @DisplayName("日期時間格式錯誤")
    class InvalidFormatTests {

        @ParameterizedTest(name = "格式錯誤：{0}")
        @ValueSource(strings = {
                "hello",
                "不是日期",
                "08/10/2026 14:30:45",
                "2026/10/08 14:30:45",
                "2026-10-08",
                "2026-10-08T14:30:45",
                "2026-10-08T14:30:45.123",
                "2026-10-08 14:30",
                "2026-10-08 14",
                "2026-10-08 14:30:45.123",
                "2026-1-8 14:30:45",
                "2026-10-08 1:30:45",
                "2026-10-08 14:3:45",
                "2026-10-08 14:30:5",
                "2026-10-08 14:30:45Z",
                " 2026-10-08 14:30:45 ",
                "",
                "   "
        })
        @DisplayName("不符合指定格式應拋出格式錯誤")
        void invalidFormat(String value) {
            assertInvalid(
                    value,
                    JacksonConfig.INVALID_DATE_TIME_MESSAGE);
        }
    }

    /** 不存在的日期 */
    @Nested
    @DisplayName("日期不存在")
    class InvalidDateTests {

        @ParameterizedTest(name = "日期不存在：{0}")
        @ValueSource(strings = {
                "2026-02-29 14:30:45",
                "2026-02-30 14:30:45",
                "2026-02-31 14:30:45",
                "2026-04-31 14:30:45",
                "2026-06-31 14:30:45",
                "2026-09-31 14:30:45",
                "2026-11-31 14:30:45",
                "2026-00-08 14:30:45",
                "2026-13-08 14:30:45",
                "2026-10-00 14:30:45",
                "2026-10-32 14:30:45",
                "1900-02-29 14:30:45"
        })
        @DisplayName("不存在的日期應拋出日期錯誤")
        void invalidDate(String value) {
            assertInvalid(
                    value,
                    JacksonConfig.INVALID_DATE_MESSAGE);
        }
    }

    /** 不存在的時間 */
    @Nested
    @DisplayName("時間不存在")
    class InvalidTimeTests {

        @ParameterizedTest(name = "時間不存在：{0}")
        @ValueSource(strings = {
                "2026-10-08 24:00:00",
                "2026-10-08 25:30:45",
                "2026-10-08 99:30:45",
                "2026-10-08 14:60:45",
                "2026-10-08 14:61:45",
                "2026-10-08 14:30:60",
                "2026-10-08 14:30:99"
        })
        @DisplayName("不存在的時間應拋出時間錯誤")
        void invalidTime(String value) {
            assertInvalid(
                    value,
                    JacksonConfig.INVALID_TIME_MESSAGE);
        }
    }

    /** JSON 型別錯誤 */
    @Nested
    @DisplayName("JSON 型別錯誤")
    class InvalidTypeTests {

        @Test
        @DisplayName("JSON 物件不可轉換為 LocalDateTime")
        void objectValue() {
            assertThrows(
                    MismatchedInputException.class,
                    () -> mapper.readValue(
                            "{\"date\":\"2026-10-08\"}",
                            LocalDateTime.class));
        }

        @Test
        @DisplayName("JSON 陣列不可轉換為 LocalDateTime")
        void arrayValue() {
            assertThrows(
                    MismatchedInputException.class,
                    () -> mapper.readValue(
                            "[\"2026-10-08 14:30:45\"]",
                            LocalDateTime.class));
        }
    }

    /** LocalDateTime 雙向轉換 */
    @Nested
    @DisplayName("LocalDateTime 雙向轉換")
    class RoundTripTests {

        @Test
        @DisplayName("序列化後反序列化應維持相同時間")
        void validRoundTrip() {
            LocalDateTime original = LocalDateTime.of(
                    2026, 10, 8, 14, 30, 45);

            String json = mapper.writeValueAsString(original);

            LocalDateTime result = mapper.readValue(
                    json,
                    LocalDateTime.class);

            assertEquals(original, result);
        }

        @Test
        @DisplayName("奈秒經過雙向轉換應被截斷")
        void nanosecondsRoundTrip() {
            LocalDateTime original = LocalDateTime.of(
                    2026, 10, 8, 14, 30, 45, 123456789);

            String json = mapper.writeValueAsString(original);

            LocalDateTime result = mapper.readValue(
                    json,
                    LocalDateTime.class);

            assertEquals(
                    original.withNano(0),
                    result);
        }

        @Test
        @DisplayName("閏年日期雙向轉換應保持一致")
        void leapYearRoundTrip() {
            LocalDateTime original = LocalDateTime.of(
                    2024, 2, 29, 23, 59, 59);

            String json = mapper.writeValueAsString(original);

            LocalDateTime result = mapper.readValue(
                    json,
                    LocalDateTime.class);

            assertEquals(original, result);
        }
    }

    /** 將字串包裝為 JSON 字串 */
    private String json(String value) {
        return mapper.writeValueAsString(value);
    }

    /** 驗證錯誤日期時間及訊息 */
    private void assertInvalid(
            String value,
            String message) {

        InvalidFormatException exception = assertThrows(
                InvalidFormatException.class,
                () -> mapper.readValue(
                        json(value),
                        LocalDateTime.class));

        assertTrue(
                exception.getOriginalMessage().contains(message),
                "預期錯誤訊息包含：" + message);
    }
}
