
package com.collection.benefitsjourney.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    /** Swagger API 基本資訊與 JWT 驗證設定 */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Benefits Journey API")
                        .version("1.0")
                        .description("電影特典地圖平台後端API，提供影城與電影資訊、特典庫存查詢、論壇和私訊功能"))

                // Swagger 分組顯示順序
                .tags(List.of(
                        new Tag().name("auth-controller"),
                        new Tag().name("user-controller"),
                        new Tag().name("影城"),
                        new Tag().name("電影"),
                        new Tag().name("特典"),
                        new Tag().name("爬蟲")))

                // JWT 驗證
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("BearerAuth"));
    }

    /** 統一 Swagger 分組順序並移除重複 Tag */
    @Bean
    public OpenApiCustomizer sortTags() {
        return openApi -> {
            List<String> order = List.of(
                    "auth-controller",
                    "user-controller",
                    "影城",
                    "電影",
                    "特典",
                    "爬蟲");

            Map<String, Tag> tags = new LinkedHashMap<>();

            // 合併重複的 Tag，保留描述
            if (openApi.getTags() != null) {
                for (Tag tag : openApi.getTags()) {
                    tags.merge(tag.getName(), tag, (oldTag, newTag) -> {
                        if (newTag.getDescription() != null) {
                            oldTag.setDescription(newTag.getDescription());
                        }
                        return oldTag;
                    });
                }
            }

            // 依照指定順序排列
            List<Tag> sorted = new ArrayList<>();

            for (String name : order) {
                Tag tag = tags.remove(name);

                if (tag != null) {
                    sorted.add(tag);
                }
            }

            // 其他未指定的分組放在最後
            sorted.addAll(tags.values());

            openApi.setTags(sorted);
        };
    }
}
