
package com.collection.benefitsjourney.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    /** 日期時間格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    /** 日期格式 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 時間格式 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    /** 格式錯誤訊息 */
    public static final String INVALID_DATE_TIME_MESSAGE = "日期時間格式錯誤，應為 yyyy-MM-dd HH:mm:ss";

    /** 日期不存在訊息 */
    public static final String INVALID_DATE_MESSAGE = "日期不存在";

    /** 時間不存在訊息 */
    public static final String INVALID_TIME_MESSAGE = "時間不存在";

    /** 自訂 LocalDateTime 的 JSON 序列化及反序列化 */
    @Bean
    JsonMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();

            /** 輸出日期時間 */
            module.addSerializer(
                    LocalDateTime.class,
                    new ValueSerializer<LocalDateTime>() {
                        @Override
                        public void serialize(
                                LocalDateTime value,
                                JsonGenerator generator,
                                SerializationContext context) {

                            generator.writeString(
                                    value.format(DATE_TIME_FORMATTER));
                        }
                    });

            /** 讀取並驗證日期時間 */
            module.addDeserializer(
                    LocalDateTime.class,
                    new ValueDeserializer<LocalDateTime>() {
                        @Override
                        public LocalDateTime deserialize(
                                JsonParser parser,
                                DeserializationContext context) {

                            String value = parser.getString();

                            // 1. 檢查格式
                            if (value == null ||
                                    !value.matches(
                                            "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {

                                return invalid(
                                        context,
                                        value,
                                        INVALID_DATE_TIME_MESSAGE);
                            }

                            // 2. 分離日期與時間
                            String date = value.substring(0, 10);
                            String time = value.substring(11);

                            // 3. 檢查日期是否存在
                            try {
                                LocalDate.parse(date, DATE_FORMATTER);
                            } catch (DateTimeParseException ex) {
                                return invalid(
                                        context,
                                        value,
                                        INVALID_DATE_MESSAGE);
                            }

                            // 4. 檢查時間是否存在
                            try {
                                LocalTime.parse(time, TIME_FORMATTER);
                            } catch (DateTimeParseException ex) {
                                return invalid(
                                        context,
                                        value,
                                        INVALID_TIME_MESSAGE);
                            }

                            // 5. 轉換為 LocalDateTime
                            return LocalDateTime.parse(
                                    value,
                                    DATE_TIME_FORMATTER);
                        }
                    });

            builder.addModule(module);
        };
    }

    /** 統一處理日期時間解析錯誤 */
    private static LocalDateTime invalid(
            DeserializationContext context,
            String value,
            String message) {

        return (LocalDateTime) context.handleWeirdStringValue(
                LocalDateTime.class,
                value,
                message);
    }
}
