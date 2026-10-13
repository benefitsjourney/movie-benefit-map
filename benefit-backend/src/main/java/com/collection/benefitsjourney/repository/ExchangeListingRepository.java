package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;

public interface ExchangeListingRepository extends JpaRepository<ExchangeListingBean, Integer> {

     // 查詢某筆持有紀錄是否已經建立交換刊登
     Optional<ExchangeListingBean> findByUseBenefitBean(UseBenefitBean useBenefitBean);

     // 查詢所有指定狀態的交換刊登
     List<ExchangeListingBean> findByStatus(String status);

     // 查詢某會員自己的交換刊登
     List<ExchangeListingBean> findByUseBenefitBean_User_UsersId(Integer usersId);

     // 查詢指定 ID 且符合指定狀態的交換刊登
     Optional<ExchangeListingBean> findByExchangeListingIdAndStatus(Integer exchangeListingId, String status);
}
