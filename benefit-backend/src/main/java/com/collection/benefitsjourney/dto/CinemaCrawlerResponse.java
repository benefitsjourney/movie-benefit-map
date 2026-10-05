package com.collection.benefitsjourney.dto;

import java.math.BigDecimal;

import com.collection.benefitsjourney.enums.County;

public record CinemaCrawlerResponse(
                String sourceId,
                String cinemaName,
                String cinemaAddress,
                BigDecimal latitude,
                BigDecimal longitude,
                String officialWebsiteUrl,
                County county,
                Boolean isActive) {
}