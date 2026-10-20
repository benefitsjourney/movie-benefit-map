package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrawlerLogRequest(

        @NotNull(groups = OnCreate.class, message = "爬蟲類型不能為空") CrawlerType crawlerType,

        @NotNull(groups = OnCreate.class, message = "執行狀態不能為空") CrawlerStatus crawlerStatus,

        @NotNull(groups = OnCreate.class, message = "抓取筆數不能為空") @Min(value = 0, message = "抓取筆數不可小於0") Integer fetchedCount,

        @NotNull(groups = OnCreate.class, message = "新增筆數不能為空") @Min(value = 0, message = "新增筆數不可小於0") Integer createdCount,

        @NotNull(groups = OnCreate.class, message = "更新筆數不能為空") @Min(value = 0, message = "更新筆數不可小於0") Integer updatedCount,

        @NotNull(groups = OnCreate.class, message = "略過筆數不能為空") @Min(value = 0, message = "略過筆數不可小於0") Integer skippedCount,

        @NotNull(groups = OnCreate.class, message = "納入審核筆數不能為空") @Min(value = 0, message = "納入審核筆數不可小於0") Integer pendingReviewCount,

        @Size(max = 1024, message = "執行訊息長度不可超過1024字元") String message,

        @NotNull(groups = OnCreate.class, message = "執行時間不能為空") @Schema(type = "string", example = "2026-10-20 10:00:00") LocalDateTime executedAt,

        @NotNull(groups = OnCreate.class, message = "結束時間不能為空") @Schema(type = "string", example = "2026-10-20 10:01:30") LocalDateTime finishedAt) {

    /** 新增時才檢查必填欄位 */
    public interface OnCreate {
    }
}
