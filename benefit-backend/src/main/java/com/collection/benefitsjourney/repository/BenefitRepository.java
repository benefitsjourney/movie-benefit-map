package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.BenefitBean;

public interface BenefitRepository extends JpaRepository<BenefitBean, Integer> {

    // GET/benefits，查詢已上架特典清單
    List<BenefitBean> findByStatus(String status, Pageable pageable);

    // GET/movies/{movieId}/benefits，透過中介表查某部電影有哪些特典
    // 可以看已上架或已下架特典
    @Query("""
            SELECT DISTINCT b FROM BenefitBean b
            JOIN CinemaMovieBenefitBean c ON c.benefitId = b.benefitId
            WHERE c.movieId = :movieId AND b.status = :status
            ORDER BY b.startDate DESC
            """)
    List<BenefitBean> findByMovieId(
    		@Param("movieId") Integer movieId,
    		@Param("status") String status,
    		Pageable pageable);


    // 爬蟲去重（同貼文+同特典名稱），避免同一則貼文被重複抓到
    boolean existsBySourceUrlAndBenefitName(String sourceUrl, String benefitName);
}