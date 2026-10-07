package com.collection.benefitsjourney.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;

public interface ExchangeListingRepository extends JpaRepository<ExchangeListingBean, Integer> {
    
	// 查詢某個會員的交換刊登?
     Optional<ExchangeListingBean> findByUseBenefitBean(UseBenefitBean useBenefitBean);
}
