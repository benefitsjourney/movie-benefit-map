package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;

public interface CinemaMovieBenefitRepository extends JpaRepository<CinemaMovieBenefitBean, Integer> {

	List<CinemaMovieBenefitBean> findByCinema_CinemaId(Integer cinemaId);

    List<CinemaMovieBenefitBean> findByMovie_MovieId(Integer movieId);

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
    List<CinemaMovieBenefitBean> findStatusByBenefit(
    		@Param("benefitId") Integer benefitId);

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
    List<CinemaMovieBenefitBean> findApprovedStatusByCinema(
    		@Param("cinemaId") Integer cinemaId);
    
    /** 一次查出多個特典各自對應的電影（列表頁顯示電影名稱用，避免每筆各查一次） */
    @Query("""
            SELECT DISTINCT c.benefit.benefitId, c.movie.movieId, c.movie.movieName
            FROM CinemaMovieBenefitBean c
            WHERE c.benefit.benefitId IN :benefitIds
            """)
    List<Object[]> findMoviesByBenefitIds(@Param("benefitIds") List<Integer> benefitIds);
}