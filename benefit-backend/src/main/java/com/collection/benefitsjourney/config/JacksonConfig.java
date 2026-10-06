package com.collection.benefitsjourney.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    /** 日期時間輸出格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 自訂LocalDateTime的JSON序列化格式 */
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

            builder.addModule(module);
        };
    }
}