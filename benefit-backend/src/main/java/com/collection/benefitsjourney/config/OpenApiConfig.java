package com.collection.benefitsjourney.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.collection.benefitsjourney.controller.AdminController;
import com.collection.benefitsjourney.controller.AuthController;
import com.collection.benefitsjourney.controller.BenefitAdminController;
import com.collection.benefitsjourney.controller.BenefitController;
import com.collection.benefitsjourney.controller.BenefitReportAdminController;
import com.collection.benefitsjourney.controller.BenefitReportController;
import com.collection.benefitsjourney.controller.CinemaController;
import com.collection.benefitsjourney.controller.CrawlerController;
import com.collection.benefitsjourney.controller.CrawlerReviewsController;
import com.collection.benefitsjourney.controller.ExcelController;
import com.collection.benefitsjourney.controller.ExchangeFavoriteController;
import com.collection.benefitsjourney.controller.ExchangeListingController;
import com.collection.benefitsjourney.controller.InventoryReportAdminController;
import com.collection.benefitsjourney.controller.InventoryReportController;
import com.collection.benefitsjourney.controller.InventoryStatusController;
import com.collection.benefitsjourney.controller.MovieController;
import com.collection.benefitsjourney.controller.TicketFavoriteController;
import com.collection.benefitsjourney.controller.TicketListingController;
import com.collection.benefitsjourney.controller.UserController;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    /** Swagger 分組顯示順序 */
    private static final List<Class<?>> CONTROLLERS = List.of(
            AuthController.class,
            UserController.class,
            AdminController.class,
            CinemaController.class,
            MovieController.class,
            BenefitController.class,
            BenefitAdminController.class,
            BenefitReportController.class,
            BenefitReportAdminController.class,
            InventoryStatusController.class,
            InventoryReportController.class,
            InventoryReportAdminController.class,
            CrawlerController.class,
            CrawlerReviewsController.class,
            TicketListingController.class,
            TicketFavoriteController.class,
            ExchangeListingController.class,
            ExchangeFavoriteController.class,
            ExcelController.class);

    private static final String SECURITY = "BearerAuth";

    /** Swagger 基本資訊與 JWT 驗證 */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Benefits Journey API")
                        .version("1.0")
                        .description("電影特典地圖平台後端API，提供影城與電影資訊、特典庫存查詢、論壇和私訊功能"))
                .components(new Components()
                        .addSecuritySchemes(SECURITY,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY));
    }

    /** 統一 Swagger 分組順序並移除重複 Tag */
    @Bean
    public OpenApiCustomizer sortTags() {
        return openApi -> {
            Map<String, Tag> tags = new LinkedHashMap<>();

            // 依照指定順序建立分組
            for (Class<?> controller : CONTROLLERS) {
                Tag tag = createTag(controller);
                tags.put(tag.getName(), tag);
            }

            // 合併 SpringDoc 產生的 Tag，保留描述
            if (openApi.getTags() != null) {
                for (Tag tag : openApi.getTags()) {
                    tags.merge(tag.getName(), tag, (oldTag, newTag) -> {
                        if (newTag.getDescription() != null
                                && !newTag.getDescription().isBlank()) {
                            oldTag.setDescription(newTag.getDescription());
                        }
                        return oldTag;
                    });
                }
            }

            // 未指定的分組自動排在最後
            openApi.setTags(new ArrayList<>(tags.values()));
        };
    }

    /** 從 Controller 取得 Swagger Tag */
    private static Tag createTag(Class<?> controller) {
        io.swagger.v3.oas.annotations.tags.Tag annotation = controller
                .getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);

        String name = annotation != null && !annotation.name().isBlank()
                ? annotation.name()
                : controller.getSimpleName()
                        .replaceAll("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", "-")
                        .toLowerCase(Locale.ROOT);

        Tag tag = new Tag().name(name);

        if (annotation != null && !annotation.description().isBlank()) {
            tag.setDescription(annotation.description());
        }

        return tag;
    }
}