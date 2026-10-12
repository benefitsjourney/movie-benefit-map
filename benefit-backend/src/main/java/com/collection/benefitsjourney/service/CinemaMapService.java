package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaMapResponse;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.LikeKeywords;

@Service
public class CinemaMapService {
    private static final double EARTH_RADIUS_METERS = 6371000;
    private static final double DEFAULT_RADIUS_METERS = 5000;

    private final CinemaRepository cinemaRepository;

    public CinemaMapService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    /**
     * 取得地圖影城資料。
     * 未提供座標：回傳全台影城。
     * 提供座標：依搜尋半徑過濾並由近到遠排序。
     */
    public CinemaMapResponse findNearby(
            String keyword,
            County county,
            Double latitude,
            Double longitude,
            Double radius) {

        validateMapQuery(latitude, longitude, radius);

        List<CinemaBean> cinemas = cinemaRepository.findAllForMap(
                LikeKeywords.toLikeKeyword(keyword),
                county);

        // 全台模式
        if (latitude == null) {
            return nationwideCinemas(cinemas);
        }

        // 附近模式
        double searchRadius = radius != null
                ? radius
                : DEFAULT_RADIUS_METERS;

        return nearbyCinemas(cinemas, latitude, longitude, searchRadius);
    }

    /** 全台模式：依影城 ID 排序，不計算距離 */
    private CinemaMapResponse nationwideCinemas(List<CinemaBean> allCinemas) {

        List<CinemaMapResponse.Cinema> cinemas = allCinemas.stream()
                .sorted(Comparator.comparing(cinema -> cinema.getCinemaId()))
                .map(cinema -> CinemaMapResponse.Cinema.from(cinema, null))
                .toList();

        return new CinemaMapResponse(
                cinemas.size(),
                null,
                null,
                null,
                cinemas);
    }

    /** 附近模式：計算距離、過濾半徑、由近到遠排序 */
    private CinemaMapResponse nearbyCinemas(
            List<CinemaBean> allCinemas,
            double latitude,
            double longitude,
            double radius) {

        List<CinemaDistance> results = new ArrayList<>();

        for (CinemaBean cinema : allCinemas) {
            double distance = distanceMeters(
                    latitude,
                    longitude,
                    cinema.getLatitude().doubleValue(),
                    cinema.getLongitude().doubleValue());

            if (distance <= radius) {
                results.add(new CinemaDistance(cinema, distance));
            }
        }

        // 使用原始距離排序，相同距離時依影城 ID 排序
        results.sort(
                Comparator.comparingDouble((CinemaDistance result) -> result.distance())
                        .thenComparing(result -> result.cinema().getCinemaId()));

        // 排序完成後再將距離四捨五入
        List<CinemaMapResponse.Cinema> cinemas = results.stream()
                .map(result -> CinemaMapResponse.Cinema.from(
                        result.cinema(),
                        Math.round(result.distance() * 10) / 10.0))
                .toList();

        return new CinemaMapResponse(
                cinemas.size(),
                latitude,
                longitude,
                radius,
                cinemas);
    }

    /** 驗證地圖搜尋參數 */
    private void validateMapQuery(
            Double latitude,
            Double longitude,
            Double radius) {

        if (latitude == null && longitude == null) {
            if (radius != null) {
                throw badRequest("未提供座標時不可指定搜尋半徑");
            }
            return;
        }

        if (latitude == null || longitude == null) {
            throw badRequest("緯度與經度必須同時提供");
        }

        if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) {
            throw badRequest("座標超出合法範圍");
        }

        if (radius != null && !isValidRadius(radius)) {
            throw badRequest("搜尋半徑必須為正數");
        }
    }

    private static boolean isValidLatitude(double latitude) {
        return Double.isFinite(latitude)
                && latitude >= -90
                && latitude <= 90;
    }

    private static boolean isValidLongitude(double longitude) {
        return Double.isFinite(longitude)
                && longitude >= -180
                && longitude <= 180;
    }

    private static boolean isValidRadius(double radius) {
        return Double.isFinite(radius) && radius > 0;
    }

    /** Haversine 直線距離，單位為公尺 */
    private double distanceMeters(
            double latitude,
            double longitude,
            double targetLatitude,
            double targetLongitude) {

        double dLat = Math.toRadians(targetLatitude - latitude);
        double dLon = Math.toRadians(targetLongitude - longitude);

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude))
                        * Math.cos(Math.toRadians(targetLatitude))
                        * Math.pow(Math.sin(dLon / 2), 2);

        return EARTH_RADIUS_METERS * 2
                * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    /** 儲存排序前的原始距離 */
    private record CinemaDistance(
            CinemaBean cinema,
            double distance) {
    }
}
