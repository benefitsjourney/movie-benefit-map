package com.collection.benefitsjourney.dto;

import java.math.BigDecimal;

import com.collection.benefitsjourney.enums.County;

public record CinemaRequest(
                String sourceId,
                String cinemaName,
                County county,
                String cinemaAddress,
                BigDecimal latitude,
                BigDecimal longitude,
                String officialWebsiteUrl,
                Boolean isActive,
                Boolean isVisible) {
}