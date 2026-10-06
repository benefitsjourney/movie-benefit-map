package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.BenefitBean;

public interface BenefitRepository extends JpaRepository<BenefitBean, Integer> {

    // GET/benefits，查詢已上架特典清單
    List<BenefitBean> findByStatus(String status);

    // GET/movies/{movieId}/benefits，查某部電影有哪些特典
    @Query("""
            SELECT DISTINCT b FROM BenefitBean b
            JOIN CinemaMovieBenefitBean c ON c.benefitId = b.benefitId
            WHERE c.movieId = :movieId
            """)
    List<BenefitBean> findByMovieId(@Param("movieId") Integer movieId, Pageable pageable);

    // 爬蟲去重，避免同一則貼文被重複抓到
    boolean existsBySourceUrlAndBenefitName(String sourceUrl, String benefitName);
}
