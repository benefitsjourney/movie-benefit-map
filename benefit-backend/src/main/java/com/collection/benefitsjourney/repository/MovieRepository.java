package com.collection.benefitsjourney.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.MovieBean;

public interface MovieRepository extends JpaRepository<MovieBean, Integer> {

    /** 依名稱與顯示狀態查詢；hasBenefit 為 true 時只留有上架特典的電影 */
    @Query("""
            SELECT m FROM MovieBean m
            WHERE (:keyword IS NULL OR m.movieName LIKE :keyword)
            AND (:isVisible IS NULL OR m.isVisible = :isVisible)
            AND (:hasBenefit = false OR EXISTS (
                SELECT 1 FROM CinemaMovieBenefitBean cmb
                WHERE cmb.movie.movieId = m.movieId
                AND cmb.benefit.status = 'APPROVED'
            ))
            """)
    Page<MovieBean> findAllByFilters(
            @Param("keyword") String keyword,
            @Param("isVisible") Boolean isVisible,
            @Param("hasBenefit") boolean hasBenefit,
            Pageable pageable);

    /** 根據來源ID取得電影 */
    Optional<MovieBean> findBySourceId(String sourceId);
}
