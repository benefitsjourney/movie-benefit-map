package com.collection.benefitsjourney.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.ReminderBean;

public interface ReminderRepository extends JpaRepository<ReminderBean, Integer> {

    /** 是否已經追蹤過這個特典 */
    boolean existsByUser_UsersIdAndBenefit_BenefitId(Integer userId, Integer benefitId);

    /** 是否已經追蹤過這部電影 */
    boolean existsByUser_UsersIdAndMovie_MovieId(Integer userId, Integer movieId);

    /** 我的追蹤清單（新到舊），特典、電影一起撈出來，避免一筆一筆查 */
    @Query("""
            SELECT r FROM ReminderBean r
            LEFT JOIN FETCH r.benefit
            LEFT JOIN FETCH r.movie
            WHERE r.user.usersId = :userId
            ORDER BY r.createdAt DESC, r.reminderId DESC
            """)
    List<ReminderBean> findMine(@Param("userId") Integer userId);

    // ===== 以下是公開行事曆用的查詢（查的是特典表、電影表，跟行事曆功能放一起比較好找）=====

    /** 公開行事曆：這段期間開始發放的特典（上架中＋已結束） */
    @Query("""
            SELECT b FROM BenefitBean b
            WHERE b.status IN ('APPROVED', 'EXPIRED')
            AND b.startDate >= :from AND b.startDate < :to
            ORDER BY b.startDate ASC, b.benefitId ASC
            """)
    List<BenefitBean> findBenefitsStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** 公開行事曆：這段期間上映、而且前台有顯示的電影 */
    @Query("""
            SELECT m FROM MovieBean m
            WHERE m.isVisible = true
            AND m.releaseDate >= :from AND m.releaseDate < :to
            ORDER BY m.releaseDate ASC, m.movieId ASC
            """)
    List<MovieBean> findMoviesReleasedBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
