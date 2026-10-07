package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.BenefitReportBean;

public interface BenefitReportRepository extends JpaRepository<BenefitReportBean, Integer> {

    // GET/benefit-reports?status=PENDING，查詢所有待審核的特典（升序，先送先審）
    List<BenefitReportBean> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    // GET/benefit-reports/me，使用者查看自己回報缺漏的特典狀態（降序，優先查看最近送出的那筆）
    List<BenefitReportBean> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // 爬蟲去重（同貼文+同特典名稱），避免同一則貼文被重複送審
    boolean existsBySourceUrlAndBenefitName(String sourceUrl, String benefitName);
}