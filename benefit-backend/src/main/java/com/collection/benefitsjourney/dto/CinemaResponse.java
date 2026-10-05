package com.collection.benefitsjourney.dto;

import java.math.BigDecimal;

import com.collection.benefitsjourney.enums.County;

import io.swagger.v3.oas.annotations.media.Schema;

public record CinemaResponse(

        @Schema(description = "影城 ID") Integer cinemaId,

        @Schema(description = "影城名稱") String cinemaName,

        @Schema(description = "影城地址") String cinemaAddress,

        @Schema(description = "緯度") BigDecimal latitude,

        @Schema(description = "經度") BigDecimal longitude,

        @Schema(description = "影城官方網站連結") String officialWebsiteUrl,

        @Schema(description = "縣市") County county,

        @Schema(description = "影城是否營業／啟用") Boolean isActive) {
}