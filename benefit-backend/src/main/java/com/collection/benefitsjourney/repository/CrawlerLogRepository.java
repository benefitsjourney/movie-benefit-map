package com.collection.benefitsjourney.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.CrawlerLogBean;
import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;

public interface CrawlerLogRepository extends JpaRepository<CrawlerLogBean, Integer> {
    /** 依條件查詢爬蟲紀錄，排序由 Pageable 帶入 */
    @Query("""
            SELECT c FROM CrawlerLogBean c
            WHERE (:crawlerType IS NULL OR c.crawlerType = :crawlerType)
            AND (:crawlerStatus IS NULL OR c.crawlerStatus = :crawlerStatus)
            """)
    Page<CrawlerLogBean> findAllByFilters(
            @Param("crawlerType") CrawlerType crawlerType,
            @Param("crawlerStatus") CrawlerStatus crawlerStatus,
            Pageable pageable);
}
