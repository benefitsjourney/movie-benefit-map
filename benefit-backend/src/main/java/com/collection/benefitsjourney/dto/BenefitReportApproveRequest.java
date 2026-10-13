package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 核准特典回報。欄位都可省略，用來補回報單缺少的資料 */
public record BenefitReportApproveRequest(
        List<Integer> cinemaIds,       // 要綁定的影城（可多間）
        Integer movieId,               // 指定電影（沒比對到電影時用）
        // Swagger用指定範例
        @Schema(
        	    type = "string",
        	    example = "2026-10-13 03:33:47",
        	    description = "日期時間格式：yyyy-MM-dd HH:mm:ss"
        	)
        LocalDateTime releaseDate) {   // 資料庫沒有該電影、要新建時必填
}
