package com.collection.benefitsjourney.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.ExchangeListingBean;

public interface ExchangeListingRepository extends JpaRepository<ExchangeListingBean, Integer> {
    // 查詢某一種狀態的交換刊登
    List<ExchangeListingBean> findByStatus(String status);

    // 查詢某個會員的交換刊登
    List<ExchangeListingBean> findByUseBenefitId(Integer useBenefitId);
}
