package com.collection.benefitsjourney.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.repository.CinemaRepository;

@Service
public class CinemaService {

    private final CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    /** 取得所有影城資料 */
    public List<CinemaResponse> findAll() {
        return cinemaRepository.findAll()
                .stream()
                .map(cinema -> CinemaResponse.from(cinema))
                .toList();
    }

    /** 新增影城 */
    public CinemaResponse createCinema(CinemaRequest request) {
        CinemaBean cinema = new CinemaBean();

        cinema.setSourceId(request.sourceId());
        cinema.setCinemaName(request.cinemaName());
        cinema.setCounty(request.county());
        cinema.setCinemaAddress(request.cinemaAddress());
        cinema.setLatitude(request.latitude());
        cinema.setLongitude(request.longitude());
        cinema.setOfficialWebsiteUrl(request.officialWebsiteUrl());

        cinema.setIsActive(true);
        cinema.setIsVisible(
                request.isVisible() != null
                        ? request.isVisible()
                        : true);

        CinemaBean savedCinema = cinemaRepository.save(cinema);

        return CinemaResponse.from(savedCinema);
    }

    /** 修改影城 */
    public CinemaResponse updateCinema(Integer id, CinemaRequest request) {

        CinemaBean cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到影城 ID：" + id));

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
            cinema.setOfficialWebsiteUrl(request.officialWebsiteUrl());
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
        return cinemaRepository.findBySourceId(sourceId)
                .map(CinemaResponse::from);
    }
}