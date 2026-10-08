package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;

public interface CinemaMovieBenefitRepository extends JpaRepository<CinemaMovieBenefitBean, Integer> {

	List<CinemaMovieBenefitBean> findByCinema_CinemaId(Integer cinemaId, Pageable pageable);

    List<CinemaMovieBenefitBean> findByMovie_MovieId(Integer movieId, Pageable pageable);

    List<CinemaMovieBenefitBean> findByBenefit_BenefitId(Integer benefitId, Pageable pageable);

    boolean existsByCinema_CinemaIdAndMovie_MovieIdAndBenefit_BenefitId(
            Integer cinemaId, Integer movieId, Integer benefitId);
}