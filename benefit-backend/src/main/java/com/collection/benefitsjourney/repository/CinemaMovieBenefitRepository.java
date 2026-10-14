package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;

public interface CinemaMovieBenefitRepository extends JpaRepository<CinemaMovieBenefitBean, Integer> {

	List<CinemaMovieBenefitBean> findByCinema_CinemaId(Integer cinemaId, Pageable pageable);

    List<CinemaMovieBenefitBean> findByMovie_MovieId(Integer movieId, Pageable pageable);

    List<CinemaMovieBenefitBean> findByBenefit_BenefitId(Integer benefitId, Pageable pageable);

    boolean existsByCinema_CinemaIdAndMovie_MovieIdAndBenefit_BenefitId(
            Integer cinemaId, Integer movieId, Integer benefitId);
    
    /** GET /benefits/{benefitId}/cinemas/status：查某特典在哪些影城有庫存 */
    @Query("""
            SELECT c FROM CinemaMovieBenefitBean c
            JOIN FETCH c.cinema ci
            JOIN FETCH c.movie
            JOIN FETCH c.benefit
            WHERE c.benefit.benefitId = :benefitId
            ORDER BY ci.cinemaId ASC, c.cinemaMovieBenefitId ASC
            """)
    Page<CinemaMovieBenefitBean> findStatusByBenefit(
    		@Param("benefitId") Integer benefitId,
    		Pageable pageable);

    /** GET /cinemas/{cinemaId}/benefits/status：查某影城目前上架中特典的庫存狀態 */
    @Query("""
            SELECT c FROM CinemaMovieBenefitBean c
            JOIN FETCH c.cinema
            JOIN FETCH c.movie
            JOIN FETCH c.benefit b
            WHERE c.cinema.cinemaId = :cinemaId
            AND b.status = 'APPROVED'
            ORDER BY b.benefitId ASC, c.cinemaMovieBenefitId ASC
            """)
    Page<CinemaMovieBenefitBean> findApprovedStatusByCinema(
    		@Param("cinemaId") Integer cinemaId,
    		Pageable pageable);
}