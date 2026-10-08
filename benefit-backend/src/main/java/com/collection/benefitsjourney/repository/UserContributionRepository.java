package com.collection.benefitsjourney.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.UserContributionBean;

public interface UserContributionRepository extends JpaRepository<UserContributionBean, Integer> {

    // 之後做庫存回報、貢獻排行時再補查詢方法
    // 刪除庫存回報時連動刪除貢獻（contributionType+referenceId）
    // 排行榜按照created_at區間統計
}