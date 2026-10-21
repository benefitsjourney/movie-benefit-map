package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.enums.County;

public interface CinemaRepository extends JpaRepository<CinemaBean, Integer> {

    /** 依名稱、縣市、營業與顯示狀態查詢；hasBenefit 為 true 時只留有上架特典的影城 */
    @Query("""
            SELECT c FROM CinemaBean c
            WHERE (:keyword IS NULL OR c.cinemaName LIKE :keyword)
            AND (:county IS NULL OR c.county = :county)
            AND (:isActive IS NULL OR c.isActive = :isActive)
            AND (:isVisible IS NULL OR c.isVisible = :isVisible)
            AND (:hasBenefit = false OR EXISTS (
                SELECT 1 FROM CinemaMovieBenefitBean cmb
                WHERE cmb.cinema.cinemaId = c.cinemaId
                AND cmb.benefit.status = 'APPROVED'
            ))
            """)
    Page<CinemaBean> findAllByFilters(
            @Param("keyword") String keyword,
            @Param("county") County county,
            @Param("isActive") Boolean isActive,
            @Param("isVisible") Boolean isVisible,
            @Param("hasBenefit") boolean hasBenefit,
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

    /** 依外部來源的影城辨認ID尋找影城 */
    Optional<CinemaBean> findBySourceId(String sourceId);
}