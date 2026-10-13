package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.BenefitBean;

public interface BenefitRepository extends JpaRepository<BenefitBean, Integer> {

	/** GET/benefits，查詢已上架特典清單 */
	Page<BenefitBean> findByStatus(String status, Pageable pageable);

    /** 
    GET/movies/{movieId}/benefits，透過中介表查某部電影有哪些特典
    可以看已上架或已下架特典
    */
    @Query(value = """
            SELECT DISTINCT b FROM BenefitBean b
            JOIN CinemaMovieBenefitBean c ON c.benefit = b
            WHERE c.movie.movieId = :movieId AND b.status = :status
            ORDER BY b.startDate DESC, b.benefitId DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT b) FROM BenefitBean b
            JOIN CinemaMovieBenefitBean c ON c.benefit = b
            WHERE c.movie.movieId = :movieId AND b.status = :status
            """)
    Page<BenefitBean> findByMovieId(
            @Param("movieId") Integer movieId,
            @Param("status") String status,
            Pageable pageable);
    
    /** 審核通過時：查這部電影有沒有「已上架、同名」的特典，有就沿用，避免重複建立 */
    @Query("""
            SELECT DISTINCT b FROM BenefitBean b
            JOIN CinemaMovieBenefitBean c ON c.benefit = b
            WHERE c.movie.movieId = :movieId
            AND b.benefitName = :benefitName
            AND b.status = 'APPROVED'
            ORDER BY b.benefitId ASC
            """)
    List<BenefitBean> findApprovedByMovieAndName(
            @Param("movieId") Integer movieId,
            @Param("benefitName") String benefitName);


    /** 爬蟲去重（同貼文+同特典名稱），避免同一則貼文被重複抓到 */
    boolean existsBySourceUrlAndBenefitName(String sourceUrl, String benefitName);
    
    
}