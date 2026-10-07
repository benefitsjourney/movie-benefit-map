package com.collection.benefitsjourney.dto;

import java.math.BigDecimal;
import java.util.List;

public record NearbyCinemaResponse(
                int total,
                double latitude,
                double longitude,
                double radius,
                List<Cinema> cinemas) {

        /** 附近影城資料 */
        public record Cinema(
                        Integer cinemaId,
                        String cinemaName,
                        String cinemaAddress,
                        BigDecimal latitude,
                        BigDecimal longitude,
                        double distance) {
        }
}