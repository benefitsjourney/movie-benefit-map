package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CinemaMovieReviewBean;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.ReviewStatus;

public interface CinemaMovieReviewsRepository extends JpaRepository<CinemaMovieReviewBean, Integer> {
    /** 依條件查詢爬蟲審核，排序由 Pageable 帶入 */
    @Query("""
            SELECT c FROM CinemaMovieReviewBean c
            WHERE (:crawlerType IS NULL OR c.crawlerType = :crawlerType)
            AND (:reviewStatus IS NULL OR c.reviewStatus = :reviewStatus)
            """)
    Page<CinemaMovieReviewBean> findAllByFilters(
            @Param("crawlerType") CinemaResourceType crawlerType,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    List<CinemaMovieReviewBean> findByCrawlerTypeAndEntityIdAndFieldName(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName);
}
