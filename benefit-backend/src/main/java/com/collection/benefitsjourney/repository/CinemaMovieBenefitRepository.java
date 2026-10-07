package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;

public interface CinemaMovieBenefitRepository extends JpaRepository<CinemaMovieBenefitBean, Integer> {

    List<CinemaMovieBenefitBean> findByCinemaId(Integer cinemaId, Pageable pageable);

    List<CinemaMovieBenefitBean> findByMovieId(Integer movieId, Pageable pageable);

    List<CinemaMovieBenefitBean> findByBenefitId(Integer benefitId, Pageable pageable);

    boolean existsByCinemaIdAndMovieIdAndBenefitId(Integer cinemaId, Integer movieId, Integer benefitId);
}