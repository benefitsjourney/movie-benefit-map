package com.collection.benefitsjourney.dto.CinemaMovie;

import java.math.BigDecimal;
import java.util.List;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.InventoryStatusResponse;
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
            Double distance,
            int benefitCount,
            List<InventoryStatusResponse> benefits) {

        public static Cinema from(
                CinemaBean cinema,
                Double distance,
                List<InventoryStatusResponse> benefits) {

            List<InventoryStatusResponse> safeBenefits = benefits == null
                    ? List.of()
                    : List.copyOf(benefits);

            return new Cinema(
                    cinema.getCinemaId(),
                    cinema.getCinemaName(),
                    cinema.getCinemaAddress(),
                    cinema.getCounty(),
                    cinema.getLatitude(),
                    cinema.getLongitude(),
                    distance,
                    safeBenefits.size(),
                    safeBenefits);
        }
    }
}