package com.collection.benefitsjourney.dto;

import java.math.BigDecimal;

import com.collection.benefitsjourney.enums.County;

public record CinemaCrawlerResponse(
                String sourceId,
                String cinemaName,
                County county,
                String cinemaAddress,
                BigDecimal latitude,
                BigDecimal longitude,
                String officialWebsiteUrl,
                Boolean isActive) {
}
