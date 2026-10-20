package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.enums.County;

public interface CinemaRepository extends JpaRepository<CinemaBean, Integer> {

    /** 依條件查詢影城 */
    @Query("""
            SELECT c FROM CinemaBean c
            WHERE (:keyword IS NULL OR c.cinemaName LIKE :keyword)
            AND (:county IS NULL OR c.county = :county)
            AND (:isActive IS NULL OR c.isActive = :isActive)
            AND (:isVisible IS NULL OR c.isVisible = :isVisible)
            """)
    Page<CinemaBean> findAllByFilters(
            @Param("keyword") String keyword,
            @Param("county") County county,
            @Param("isActive") Boolean isActive,
            @Param("isVisible") Boolean isVisible,
            Pageable pageable);

    /** 取得地圖可顯示的影城，名稱與縣市條件與列表查詢相同 */
    @Query("""
            SELECT c FROM CinemaBean c
            WHERE c.isActive = true
            AND c.isVisible = true
            AND c.latitude IS NOT NULL
            AND c.longitude IS NOT NULL
            AND (:keyword IS NULL OR c.cinemaName LIKE :keyword)
            AND (:county IS NULL OR c.county = :county)
            """)
    List<CinemaBean> findAllForMap(
            @Param("keyword") String keyword,
            @Param("county") County county);

    /** 一次取出指定影城的上架特典，電影與特典名稱為 null 時不篩選，有值時以 LIKE 模糊比對 */
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
    List<CinemaMovieBenefitBean> findApprovedForMap(
            @Param("cinemaIds") List<Integer> cinemaIds,
            @Param("movieKeyword") String movieKeyword,
            @Param("benefitKeyword") String benefitKeyword);

    /** 依外部來源的影城辨認ID尋找影城 */
    Optional<CinemaBean> findBySourceId(String sourceId);
}