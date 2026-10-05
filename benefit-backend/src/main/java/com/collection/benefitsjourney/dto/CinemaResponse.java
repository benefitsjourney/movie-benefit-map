package com.collection.benefitsjourney.dto;

import java.math.BigDecimal;

import com.collection.benefitsjourney.enums.County;

public record CinemaResponse(
                Integer cinemaId,
                String cinemaName,
                County county,
                String cinemaAddress,
                BigDecimal latitude,
                BigDecimal longitude,
                String officialWebsiteUrl,
                Boolean isActive) {
}
