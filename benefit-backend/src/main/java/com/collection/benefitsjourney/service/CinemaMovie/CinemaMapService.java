package com.collection.benefitsjourney.service.CinemaMovie;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMapResponse;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.LikeKeywords;

@Service
public class CinemaMapService {
    private static final double EARTH_RADIUS_METERS = 6371000;

    private final CinemaRepository cinemaRepository;
    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;

    public CinemaMapService(
            CinemaRepository cinemaRepository,
            CinemaMovieBenefitRepository cinemaMovieBenefitRepository) {
        this.cinemaRepository = cinemaRepository;
        this.cinemaMovieBenefitRepository = cinemaMovieBenefitRepository;
    }

    /** 依座標及半徑搜尋影城，套用特典條件並依距離排序 */
    public CinemaMapResponse findNearby(
            String keyword,
            County county,
            Double latitude,
            Double longitude,
            Double radius,
            String movieKeyword,
            String benefitKeyword,
            Boolean hasBenefit) {

        // 驗證座標與搜尋半徑
        validateMapQuery(latitude, longitude, radius);

        // 查詢符合影城名稱、縣市條件的影城
        List<CinemaBean> allCinemas = cinemaRepository.findAllForMap(
                LikeKeywords.toLikeKeyword(keyword), county);

        // 計算距離，只保留搜尋半徑內的影城
        List<CinemaDistance> withinRadius = new ArrayList<>();
        for (CinemaBean cinema : allCinemas) {
            double distance = distanceMeters(
                    latitude, longitude,
                    cinema.getLatitude().doubleValue(),
                    cinema.getLongitude().doubleValue());

            if (distance <= radius) {
                withinRadius.add(new CinemaDistance(cinema, distance));
            }
        }

        // 依距離排序，距離相同時依影城 ID 排序
        withinRadius.sort(Comparator
                .comparingDouble((CinemaDistance result) -> result.distance())
                .thenComparing(result -> result.cinema().getCinemaId()));

        // 取得範圍內影城 ID，供特典查詢使用
        List<Integer> cinemaIds = withinRadius.stream()
                .map(result -> result.cinema().getCinemaId())
                .toList();

        // 查上架特典。有特典、電影名或特典名條件時，沒有符合結果的影城不留下
        CinemaBenefitLookup.Group benefitsByCinema = CinemaBenefitLookup.load(
                cinemaMovieBenefitRepository,
                cinemaIds,
                movieKeyword,
                benefitKeyword,
                hasBenefit);

        // 組裝影城資料，距離取到小數第一位
        List<CinemaMapResponse.Cinema> cinemas = new ArrayList<>();
        for (CinemaDistance result : withinRadius) {
            Integer cinemaId = result.cinema().getCinemaId();
            if (!benefitsByCinema.keep(cinemaId)) {
                continue;
            }
            cinemas.add(CinemaMapResponse.Cinema.from(
                    result.cinema(),
                    Math.round(result.distance() * 10) / 10.0,
                    benefitsByCinema.of(cinemaId)));
        }

        // 回傳影城數量、搜尋條件與結果
        return new CinemaMapResponse(
                cinemas.size(), latitude, longitude, radius, List.copyOf(cinemas));
    }

    /** 驗證必填、座標範圍及半徑合法性 */
    private void validateMapQuery(Double latitude, Double longitude, Double radius) {
        // 座標與半徑不可為空
        if (latitude == null || longitude == null || radius == null) {
            throw badRequest("緯度、經度與搜尋半徑為必填");
        }

        // 座標須為有限數值，且落在合法經緯度範圍
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw badRequest("座標超出合法範圍");
        }

        // 搜尋半徑須為有限正數
        if (!Double.isFinite(radius) || radius <= 0) {
            throw badRequest("搜尋半徑必須為正數");
        }
    }

    /** 使用 Haversine 公式計算兩座標間的直線距離（公尺） */
    private double distanceMeters(
            double latitude,
            double longitude,
            double targetLatitude,
            double targetLongitude) {

        // 將經緯度差轉換為弧度
        double dLat = Math.toRadians(targetLatitude - latitude);
        double dLon = Math.toRadians(targetLongitude - longitude);

        // 計算 Haversine 公式中的球面距離參數
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude))
                        * Math.cos(Math.toRadians(targetLatitude))
                        * Math.pow(Math.sin(dLon / 2), 2);

        // 換算成公尺，並避免浮點誤差使 asin 超出範圍
        return EARTH_RADIUS_METERS * 2
                * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    /** 暫存影城與未四捨五入的距離，供排序使用 */
    private record CinemaDistance(CinemaBean cinema, double distance) {
    }
}
