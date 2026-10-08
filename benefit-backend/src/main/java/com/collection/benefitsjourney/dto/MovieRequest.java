package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovieRequest(

        @Size(max = 64, message = "來源ID長度不可超過64字元") String sourceId,

        @Size(max = 64, message = "電影名稱長度不可超過64字元") @NotBlank(groups = OnCreate.class, message = "電影名稱不能為空") String movieName,

        @Size(max = 512, message = "電影圖片長度不可超過512字元") String imageUrl,

        Boolean isVisible,

        @Schema(type = "string", example = "2026-10-08 10:33:00") LocalDateTime releaseDate) {

    /** 新增電影時才檢查必填欄位；修改時允許省略未變更欄位 */
    public interface OnCreate {
    }
}
