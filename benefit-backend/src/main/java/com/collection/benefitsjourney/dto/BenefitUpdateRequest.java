package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.enums.BenefitType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 管理員修改正式特典
 * 欄位皆可省略，只更新有帶的欄位（沒有就不修改）
 * 不開放修改status
 */
public record BenefitUpdateRequest(

        @Size(max = 100, message = "特典名稱長度不可超過100字元") String benefitName,
        // 可不帶
        Boolean isMemberOnly,

        BenefitType benefitType,

        @Schema(type = "string", example = "2026-10-14 00:00:00") LocalDateTime startDate,

        @Schema(type = "string", example = "2026-10-20 23:59:59") LocalDateTime endDate,

        @Size(max = 200, message = "官方圖片網址長度不可超過200字元") String officialImageUrl,

        // 換圖時要連同新的publicId一起帶，之後刪除Cloudinary圖片才找得到
        @Size(max = 200, message = "圖片publicId長度不可超過200字元") String publicId,

        @Size(max = 300, message = "來源網址長度不可超過300字元") String sourceUrl) {
}
