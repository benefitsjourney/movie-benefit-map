package com.collection.benefitsjourney.dto.CinemaMovie;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.enums.County;

public record CinemaResponse(
        Integer cinemaId,
        String sourceId,
        String cinemaName,
        County county,
        String cinemaAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String officialWebsiteUrl,
        Boolean isActive,
        Boolean isVisible,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CinemaResponse from(CinemaBean cinema) {
        return new CinemaResponse(
                cinema.getCinemaId(),
                cinema.getSourceId(),
                cinema.getCinemaName(),
                cinema.getCounty(),
                cinema.getCinemaAddress(),
                cinema.getLatitude(),
                cinema.getLongitude(),
                cinema.getOfficialWebsiteUrl(),
                cinema.getIsActive(),
                cinema.getIsVisible(),
                cinema.getCreatedAt(),
                cinema.getUpdatedAt());
    }
}