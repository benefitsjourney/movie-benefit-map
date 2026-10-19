package com.collection.benefitsjourney.dto.CinemaMovie;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.ReviewStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CinemaMovieReviewRequest(

        @NotNull(groups = OnCreate.class, message = "爬蟲類型不能為空") CinemaResourceType crawlerType,

        @NotNull(groups = OnCreate.class, message = "對應資料ID不能為空") Integer entityId,

        @Size(max = 64, message = "欄位名稱長度不可超過64字元") @NotBlank(groups = OnCreate.class, message = "欄位名稱不能為空") String fieldName,

        @Size(max = 512, message = "爬蟲值長度不可超過512字元") String crawlerValue,

        @Size(max = 512, message = "原值長度不可超過512字元") String previousValue,

        ReviewStatus reviewStatus,

        @Schema(type = "string", example = "2026-10-08 10:33:00") LocalDateTime reviewedAt) {

    /** 新增時才檢查必填欄位；修改時允許省略未變更欄位 */
    public interface OnCreate {
    }
}
