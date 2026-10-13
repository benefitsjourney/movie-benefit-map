package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CrawlerReviewBean;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.ReviewStatus;

public interface CrawlerReviewsRepository extends JpaRepository<CrawlerReviewBean, Integer> {
    /** 依條件查詢爬蟲審核，排序由 Pageable 帶入 */
    @Query("""
            SELECT c FROM CrawlerReviewBean c
            WHERE (:crawlerType IS NULL OR c.crawlerType = :crawlerType)
            AND (:reviewStatus IS NULL OR c.reviewStatus = :reviewStatus)
            """)
    Page<CrawlerReviewBean> findAllByFilters(
            @Param("crawlerType") CinemaResourceType crawlerType,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable);

    List<CrawlerReviewBean> findByCrawlerTypeAndEntityIdAndFieldName(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName);
}
