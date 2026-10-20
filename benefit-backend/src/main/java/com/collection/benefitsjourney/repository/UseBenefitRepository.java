package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.UseBenefitBean;

public interface UseBenefitRepository extends JpaRepository<UseBenefitBean, Integer> {

	/**
     * GET /use-benefits/me：查詢某個使用者尚未軟刪除的收藏紀錄，並且一次取得每筆收藏對應的特典與照片（新到舊）
     * 一次把特典和照片一起撈出來，避免每筆再各查一次（N+1）
     * 不管特典有沒有下架都會列出來
     */
    @Query("""
            SELECT DISTINCT u FROM UseBenefitBean u
            JOIN FETCH u.benefit
            LEFT JOIN FETCH u.photos
            WHERE u.user.usersId = :userId
            AND u.deletedAt IS NULL
            ORDER BY u.createdAt DESC, u.useBenefitId DESC
            """)
    List<UseBenefitBean> findMyUseBenefits(@Param("userId") Integer userId);

    /** 檢查這個使用者是不是已經持有這個特典（同一個特典只能有一筆持有紀錄） */
    boolean existsByUser_UsersIdAndBenefit_BenefitIdAndDeletedAtIsNull(Integer userId, Integer benefitId);
	
}