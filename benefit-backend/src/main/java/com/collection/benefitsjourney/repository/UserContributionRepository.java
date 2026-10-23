package com.collection.benefitsjourney.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.UserContributionBean;

public interface UserContributionRepository extends JpaRepository<UserContributionBean, Integer> {

    /** 刪除指定回報的貢獻紀錄（刪庫存回報、刪特典時連動） */
    void deleteByContributionTypeAndReferenceId(String contributionType, Integer referenceId);

    /**
     * 貢獻排行：統計 since 之後每位會員的貢獻次數（只算 ACTIVE 會員）
     * 每一列是 Object[]：
     *   [0] usersId、[1] name、[2] account、
     *   [3] 庫存回報次數、[4] 缺漏回報次數、[5] 總次數
     * 排序：總次數多的在前，一樣多就照會員編號
     */
    @Query("""
            SELECT u.usersId,
                   u.name,
                   u.account,
                   SUM(CASE WHEN c.contributionType = 'INVENTORY_REPORT' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN c.contributionType = 'BENEFIT_REPORT' THEN 1 ELSE 0 END),
                   COUNT(c)
            FROM UserContributionBean c
            JOIN c.user u
            WHERE c.createdAt >= :since
              AND (u.status IS NULL OR u.status = 'ACTIVE')
            GROUP BY u.usersId, u.name, u.account
            ORDER BY COUNT(c) DESC, u.usersId ASC
            """)
    List<Object[]> countByUserSince(@Param("since") LocalDateTime since);
}
