package com.collection.benefitsjourney.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.InventoryReportBean;

public interface InventoryReportRepository extends JpaRepository<InventoryReportBean, Integer> {

    /** 回報頻率限制：使用者在這個影城特典，since之後有沒有回報過 */
    boolean existsByUser_UsersIdAndCinemaMovieBenefit_CinemaMovieBenefitIdAndReportedAtAfter(
            Integer userId, Integer cinemaMovieBenefitId, LocalDateTime since);

    /** 該影城特典「回報時間最新」的一筆，用來決定中介表要顯示的狀態 */
    Optional<InventoryReportBean> findFirstByCinemaMovieBenefit_CinemaMovieBenefitIdOrderByReportedAtDescReportIdDesc(
            Integer cinemaMovieBenefitId);

    /** 我的庫存回報（新到舊），一次帶出影城、電影、特典名稱避免N+1 */
    @Query(value = """
            SELECT r FROM InventoryReportBean r
            JOIN FETCH r.cinemaMovieBenefit c
            JOIN FETCH c.cinema
            JOIN FETCH c.movie
            JOIN FETCH c.benefit
            WHERE r.user.usersId = :userId
            ORDER BY r.reportedAt DESC, r.reportId DESC
            """, countQuery = """
            SELECT COUNT(r) FROM InventoryReportBean r
            WHERE r.user.usersId = :userId
            """)
    Page<InventoryReportBean> findMyReports(@Param("userId") Integer userId, Pageable pageable);
}
