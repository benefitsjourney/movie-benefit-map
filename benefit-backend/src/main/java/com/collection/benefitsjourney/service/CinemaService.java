package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.repository.CinemaRepository;

@Service
public class CinemaService {
    private CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    public List<CinemaBean> findAll() {
        return cinemaRepository.findAll();
    }
}
