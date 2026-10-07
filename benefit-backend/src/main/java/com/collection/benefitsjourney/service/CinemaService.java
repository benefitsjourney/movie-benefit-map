package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.conflict;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
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
            // sourceId 有修改時檢查是否與其他影城重複
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

    /** 根據來源 ID 取得影城 */
    public Optional<CinemaResponse> findBySourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return Optional.empty();
        }

        return cinemaRepository.findBySourceId(sourceId)
                .map(CinemaResponse::from);
    }
}