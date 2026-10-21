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

    /** 一次取出多間影城的上架特典。電影與特典名稱為 null 時不篩選，有值時以 LIKE 模糊比對 */
    @Query("""
            SELECT c FROM CinemaMovieBenefitBean c
            JOIN FETCH c.cinema ci
            JOIN FETCH c.movie m
            JOIN FETCH c.benefit b
            WHERE ci.cinemaId IN :cinemaIds
            AND b.status = 'APPROVED'
            AND (:movieKeyword IS NULL OR m.movieName LIKE :movieKeyword)
            AND (:benefitKeyword IS NULL OR b.benefitName LIKE :benefitKeyword)
            ORDER BY ci.cinemaId ASC, b.benefitId ASC, c.cinemaMovieBenefitId ASC
            """)
    List<CinemaMovieBenefitBean> findApprovedByCinemaIds(
            @Param("cinemaIds") List<Integer> cinemaIds,
            @Param("movieKeyword") String movieKeyword,
            @Param("benefitKeyword") String benefitKeyword);

    /** 一次取出多部電影的上架特典 */
    @Query("""
            SELECT c FROM CinemaMovieBenefitBean c
            JOIN FETCH c.cinema ci
            JOIN FETCH c.movie m
            JOIN FETCH c.benefit b
            WHERE m.movieId IN :movieIds
            AND b.status = 'APPROVED'
            ORDER BY m.movieId ASC, b.benefitId ASC, c.cinemaMovieBenefitId ASC
            """)
    List<CinemaMovieBenefitBean> findApprovedByMovieIds(
            @Param("movieIds") List<Integer> movieIds);

    /** 前台影城列表的上架特典筆數 */
    @Query("""
            SELECT c.cinema.cinemaId, COUNT(c)
            FROM CinemaMovieBenefitBean c
            WHERE c.cinema.cinemaId IN :cinemaIds
            AND c.benefit.status = 'APPROVED'
            GROUP BY c.cinema.cinemaId
            """)
    List<Object[]> countApprovedByCinemaIds(@Param("cinemaIds") List<Integer> cinemaIds);

    /** 前台電影列表的上架特典筆數 */
    @Query("""
            SELECT c.movie.movieId, COUNT(c)
            FROM CinemaMovieBenefitBean c
            WHERE c.movie.movieId IN :movieIds
            AND c.benefit.status = 'APPROVED'
            GROUP BY c.movie.movieId
            """)
    List<Object[]> countApprovedByMovieIds(@Param("movieIds") List<Integer> movieIds);

    /** 一次查出多個特典各自對應的電影（列表頁顯示電影名稱用，避免每筆各查一次） */
    @Query("""
            SELECT DISTINCT c.benefit.benefitId, c.movie.movieId, c.movie.movieName
            FROM CinemaMovieBenefitBean c
            WHERE c.benefit.benefitId IN :benefitIds
            """)
    List<Object[]> findMoviesByBenefitIds(@Param("benefitIds") List<Integer> benefitIds);
}