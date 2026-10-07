package com.collection.benefitsjourney.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.MovieBean;

public interface MovieRepository extends JpaRepository<MovieBean, Integer> {

    /** 依條件查詢電影 */
    @Query("""
            SELECT m FROM MovieBean m
            WHERE (:keyword IS NULL OR m.movieName LIKE :keyword)
            AND (:isVisible IS NULL OR m.isVisible = :isVisible)
            """)
    Page<MovieBean> findAllByFilters(
            @Param("keyword") String keyword,
            @Param("isVisible") Boolean isVisible,
            Pageable pageable);

    /** 根據來源ID取得電影 */
    Optional<MovieBean> findBySourceId(String sourceId);
}
