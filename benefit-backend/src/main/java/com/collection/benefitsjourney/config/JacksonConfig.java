package com.collection.benefitsjourney.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

    /** 日期時間輸出格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 日期時間無法解析時回給呼叫端的說明 */
    public static final String INVALID_DATE_TIME_MESSAGE = "日期時間格式應為 yyyy-MM-dd HH:mm:ss";

    /** 自訂LocalDateTime的JSON格式，輸出與輸入都使用 yyyy-MM-dd HH:mm:ss */
    @Bean
    JsonMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();

            module.addSerializer(
                    LocalDateTime.class,
                    new ValueSerializer<LocalDateTime>() {
                        /** 將LocalDateTime轉換為指定格式字串 */
                        @Override
                        public void serialize(
                                LocalDateTime value,
                                JsonGenerator generator,
                                SerializationContext context) {

                            generator.writeString(value.format(DATE_TIME_FORMATTER));
                        }
                    });

            module.addDeserializer(
                    LocalDateTime.class,
                    new ValueDeserializer<LocalDateTime>() {
                        /** 讀取 yyyy-MM-dd HH:mm:ss，並相容 ISO-8601 的 T 分隔 */
                        @Override
                        public LocalDateTime deserialize(
                                JsonParser parser,
                                DeserializationContext context) {

                            String text = parser.getString();
                            if (text == null || text.isBlank()) {
                                return null;
                            }

                            String value = text.trim();
                            try {
                                if (value.length() == 10) {
                                    return LocalDate.parse(value).atStartOfDay();
                                }
                                if (value.indexOf('T') >= 0) {
                                    return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                }
                                return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
                            } catch (DateTimeParseException ex) {
                                return (LocalDateTime) context.handleWeirdStringValue(
                                        LocalDateTime.class,
                                        value,
                                        INVALID_DATE_TIME_MESSAGE);
                            }
                        }
                    });

            builder.addModule(module);
        };
    }
}