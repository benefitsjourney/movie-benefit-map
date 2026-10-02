package com.collection.benefitsjourney.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Benefits Journey API")
                        .version("1.0")
                        .description("電影特典地圖平台後端API，提供影城與電影資訊、特典庫存查詢、交換私訊功能"));
    }
}