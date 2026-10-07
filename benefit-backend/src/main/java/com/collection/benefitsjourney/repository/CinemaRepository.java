package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.enums.County;

public interface CinemaRepository extends JpaRepository<CinemaBean, Integer> {
    /** 依條件查詢影城 */
    @Query("""
            SELECT c FROM CinemaBean c
            WHERE (:keyword IS NULL OR c.cinemaName LIKE CONCAT('%', :keyword, '%'))
            AND (:county IS NULL OR c.county = :county)
            AND (:isActive IS NULL OR c.isActive = :isActive)
            AND (:isVisible IS NULL OR c.isVisible = :isVisible)
            """)
    List<CinemaBean> findAllByFilters(
            @Param("keyword") String keyword,
            @Param("county") County county,
            @Param("isActive") Boolean isActive,
            @Param("isVisible") Boolean isVisible);

    /** 依外部來源的影城辨認ID尋找影城 */
    Optional<CinemaBean> findBySourceId(String sourceId);
}
