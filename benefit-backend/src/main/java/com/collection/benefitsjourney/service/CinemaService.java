package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaCreateRequest;
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
    public CinemaResponse createCinema(CinemaCreateRequest request) {
        CinemaBean cinema = new CinemaBean();

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
}