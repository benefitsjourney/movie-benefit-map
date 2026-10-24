package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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


    /** 重複匯入同一則貼文時，替還沒有圖片的特典補圖 */
    List<BenefitBean> findBySourceUrlAndBenefitName(String sourceUrl, String benefitName);
    
    /** 爬蟲去重：同一部電影、同名、同開始日已經有正式特典（不同貼文重複宣傳同一個特典） */
    @Query("""
            SELECT COUNT(DISTINCT b) FROM BenefitBean b
            JOIN CinemaMovieBenefitBean c ON c.benefit = b
            WHERE c.movie.movieId = :movieId
            AND b.benefitName = :benefitName
            AND b.startDate = :startDate
            """)
    long countByMovieAndNameAndStartDate(
            @Param("movieId") Integer movieId,
            @Param("benefitName") String benefitName,
            @Param("startDate") java.time.LocalDateTime startDate);
    
    
    // ===== 刪除特典 =====
    // 刪除順序：貢獻→庫存回報→中介表→回報單解除關聯→特典本身

    /** 有幾位會員的「持有紀錄」用到這個特典（有的話不允許刪除） */
    @Query("SELECT COUNT(u) FROM UseBenefitBean u WHERE u.benefit.benefitId = :benefitId")
    long countUseBenefits(@Param("benefitId") Integer benefitId);

    /** 1. 刪除這個特典所有庫存回報對應的貢獻紀錄 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM UserContributionBean uc
            WHERE uc.contributionType = 'INVENTORY_REPORT'
            AND uc.referenceId IN (
                SELECT r.reportId FROM InventoryReportBean r
                WHERE r.cinemaMovieBenefit.cinemaMovieBenefitId IN (
                    SELECT c.cinemaMovieBenefitId FROM CinemaMovieBenefitBean c
                    WHERE c.benefit.benefitId = :benefitId))
            """)
    int deleteInventoryContributionsByBenefit(@Param("benefitId") Integer benefitId);

    /** 2. 刪除這個特典所有庫存回報 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM InventoryReportBean r
            WHERE r.cinemaMovieBenefit.cinemaMovieBenefitId IN (
                SELECT c.cinemaMovieBenefitId FROM CinemaMovieBenefitBean c
                WHERE c.benefit.benefitId = :benefitId)
            """)
    int deleteInventoryReportsByBenefit(@Param("benefitId") Integer benefitId);

    /** 3. 刪除中介表 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CinemaMovieBenefitBean c WHERE c.benefit.benefitId = :benefitId")
    int deleteLinksByBenefit(@Param("benefitId") Integer benefitId);

    /** 4. 回報單保留（歷史紀錄不刪），只把指向這個特典的關聯清掉 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE BenefitReportBean r SET r.benefit = NULL WHERE r.benefit.benefitId = :benefitId")
    int detachReportsFromBenefit(@Param("benefitId") Integer benefitId);
    
    /** 爬蟲同款比對：這部電影的所有特典（上架中、已下架都算），用來判斷同款、追加印製、疑似同款 */
    @Query("""
            SELECT DISTINCT b FROM BenefitBean b
            JOIN CinemaMovieBenefitBean c ON c.benefit = b
            WHERE c.movie.movieId = :movieId
            ORDER BY b.benefitId ASC
            """)
    List<BenefitBean> findAllByMovieId(@Param("movieId") Integer movieId);

    /** 自動下架：上架中、有結束日、而且結束日已經過了 */
    List<BenefitBean> findByStatusAndEndDateBefore(String status, java.time.LocalDateTime now);

    /** 自動下架：上架中、送完為止（沒有結束日）、開始日早於cutoff（開始日起20週） */
    List<BenefitBean> findByStatusAndEndDateIsNullAndStartDateBefore(String status, java.time.LocalDateTime cutoff);

    /**
     * GET /benefits、GET /benefits/admin：特典列表搜尋（前台、後台共用）
     * status：APPROVED／EXPIRED；keyword搜特典名稱或電影名稱；status、keyword、benefitType傳null代表不篩選
     */
    @Query(value = """
            SELECT b FROM BenefitBean b
            WHERE (:status IS NULL OR b.status = :status)
            AND (:benefitType IS NULL OR b.benefitType = :benefitType)
            AND (:keyword IS NULL
                 OR b.benefitName LIKE :keyword
                 OR EXISTS (SELECT 1 FROM CinemaMovieBenefitBean c
                            WHERE c.benefit = b AND c.movie.movieName LIKE :keyword))
            """, countQuery = """
            SELECT COUNT(b) FROM BenefitBean b
            WHERE (:status IS NULL OR b.status = :status)
            AND (:benefitType IS NULL OR b.benefitType = :benefitType)
            AND (:keyword IS NULL
                 OR b.benefitName LIKE :keyword
                 OR EXISTS (SELECT 1 FROM CinemaMovieBenefitBean c
                            WHERE c.benefit = b AND c.movie.movieName LIKE :keyword))
            """)
    Page<BenefitBean> searchBenefits(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("benefitType") String benefitType,
            Pageable pageable);
    
    
}

