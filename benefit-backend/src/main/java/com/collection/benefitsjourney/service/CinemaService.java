package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.conflict;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.dto.NearbyCinemaResponse;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.repository.CinemaRepository;

@Service
public class CinemaService {

    private final CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    /** 取得所有影城資料 */
    public List<CinemaResponse> findAll(
            String keyword,
            County county,
            Boolean isActive,
            Boolean isVisible) {
        return cinemaRepository.findAllByFilters(
                keyword == null || keyword.isBlank() ? null : keyword,
                county,
                isActive,
                isVisible)
                .stream()
                .map(CinemaResponse::from)
                .toList();
    }

    /** 取得指定id影城資料 */
    public CinemaResponse findById(Integer id) {
        CinemaBean cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> notFound("找不到影城 ID：" + id));

        return CinemaResponse.from(cinema);
    }

    /** 新增影城 */
    public CinemaResponse createCinema(CinemaRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.cinemaName() == null
                || request.cinemaName().isBlank()) {
            throw badRequest("影城名稱不可為空");
        }

        if (request.county() == null) {
            throw badRequest("縣市不可為空");
        }

        if (request.sourceId() != null
                && cinemaRepository.findBySourceId(request.sourceId()).isPresent()) {
            throw conflict("來源 ID 已存在：" + request.sourceId());
        }

        CinemaBean cinema = new CinemaBean();

        cinema.setSourceId(request.sourceId());
        cinema.setCinemaName(request.cinemaName());
        cinema.setCounty(request.county());
        cinema.setCinemaAddress(request.cinemaAddress());
        cinema.setLatitude(request.latitude());
        cinema.setLongitude(request.longitude());
        cinema.setOfficialWebsiteUrl(request.officialWebsiteUrl());

        cinema.setIsActive(
                request.isActive() != null
                        ? request.isActive()
                        : true);

        cinema.setIsVisible(
                request.isVisible() != null
                        ? request.isVisible()
                        : true);

        CinemaBean savedCinema = cinemaRepository.save(cinema);

        return CinemaResponse.from(savedCinema);
    }

    /** 修改影城 */
    public CinemaResponse updateCinema(Integer id, CinemaRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.cinemaName() != null
                && request.cinemaName().isBlank()) {
            throw badRequest("影城名稱不可為空");
        }

        CinemaBean cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> notFound("找不到影城 ID：" + id));

        if (request.sourceId() != null) {
            // sourceId有修改時檢查是否與其他影城重複
            cinemaRepository.findBySourceId(request.sourceId())
                    .filter(existing -> !existing.getCinemaId().equals(id))
                    .ifPresent(existing -> {
                        throw conflict(
                                "來源 ID 已存在：" + request.sourceId());
                    });

            cinema.setSourceId(request.sourceId());
        }

        if (request.cinemaName() != null) {
            cinema.setCinemaName(request.cinemaName());
        }

        if (request.cinemaAddress() != null) {
            cinema.setCinemaAddress(request.cinemaAddress());
        }

        if (request.latitude() != null) {
            cinema.setLatitude(request.latitude());
        }

        if (request.longitude() != null) {
            cinema.setLongitude(request.longitude());
        }

        if (request.officialWebsiteUrl() != null) {
            cinema.setOfficialWebsiteUrl(
                    request.officialWebsiteUrl());
        }

        if (request.county() != null) {
            cinema.setCounty(request.county());
        }

        if (request.isActive() != null) {
            cinema.setIsActive(request.isActive());
        }

        if (request.isVisible() != null) {
            cinema.setIsVisible(request.isVisible());
        }

        CinemaBean savedCinema = cinemaRepository.save(cinema);

        return CinemaResponse.from(savedCinema);
    }

    /** 根據來源ID取得影城 */
    public Optional<CinemaResponse> findBySourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return Optional.empty();
        }

        return cinemaRepository.findBySourceId(sourceId)
                .map(CinemaResponse::from);
    }

    /** 搜尋指定座標附近的影城 */
    public NearbyCinemaResponse findNearby(
            double latitude, double longitude, double radius) {

        List<NearbyCinemaResponse.Cinema> cinemas = new ArrayList<>();

        // 查詢啟用且可見的影城
        List<CinemaBean> allCinemas = cinemaRepository
                .findAllByFilters(null, null, true, true);

        for (CinemaBean cinema : allCinemas) {
            // 排除沒有經緯度的影城
            if (cinema.getLatitude() == null || cinema.getLongitude() == null) {
                continue;
            }

            double lat = cinema.getLatitude().doubleValue();
            double lon = cinema.getLongitude().doubleValue();

            // 計算兩個座標之間的距離（公尺）
            double dLat = Math.toRadians(lat - latitude);
            double dLon = Math.toRadians(lon - longitude);

            double a = Math.pow(Math.sin(dLat / 2), 2)
                    + Math.cos(Math.toRadians(latitude))
                            * Math.cos(Math.toRadians(lat))
                            * Math.pow(Math.sin(dLon / 2), 2);

            double distance = 6371000 * 2
                    * Math.asin(Math.sqrt(Math.min(1, a)));

            // 排除超出搜尋半徑的影城
            if (distance > radius) {
                continue;
            }

            // 距離四捨五入至小數點第一位
            distance = Math.round(distance * 10) / 10.0;

            // 加入符合條件的影城
            cinemas.add(new NearbyCinemaResponse.Cinema(
                    cinema.getCinemaId(),
                    cinema.getCinemaName(),
                    cinema.getCinemaAddress(),
                    cinema.getLatitude(),
                    cinema.getLongitude(),
                    distance));
        }

        // 按距離由近到遠排序
        cinemas.sort(Comparator.comparingDouble(NearbyCinemaResponse.Cinema::distance));

        return new NearbyCinemaResponse(
                cinemas.size(),
                latitude,
                longitude,
                radius,
                cinemas);
    }
}