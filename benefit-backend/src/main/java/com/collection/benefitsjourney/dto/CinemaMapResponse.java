package com.collection.benefitsjourney.dto;

import java.math.BigDecimal;
import java.util.List;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.enums.County;

public record CinemaMapResponse(
        int total,
        Double latitude,
        Double longitude,
        Double radius,
        List<Cinema> cinemas) {

    public record Cinema(
            Integer cinemaId,
            String cinemaName,
            String cinemaAddress,
            County county,
            BigDecimal latitude,
            BigDecimal longitude,
            Double distance) {

        public static Cinema from(CinemaBean cinema, Double distance) {
            return new Cinema(
                    cinema.getCinemaId(),
                    cinema.getCinemaName(),
                    cinema.getCinemaAddress(),
                    cinema.getCounty(),
                    cinema.getLatitude(),
                    cinema.getLongitude(),
                    distance);
        }
    }
}