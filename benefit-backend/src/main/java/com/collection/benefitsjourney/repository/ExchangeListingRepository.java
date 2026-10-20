package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

     // 公開查詢交換刊登：
     // 刊登標題、會員姓名、特典名稱、特典類型彼此 OR。
     // 四項條件都沒有輸入時，查詢該狀態的全部刊登。
     @Query("""
               SELECT listing
               FROM ExchangeListingBean listing
               LEFT JOIN listing.useBenefitBean useBenefit
               LEFT JOIN useBenefit.user users
               LEFT JOIN useBenefit.benefit benefit
               WHERE listing.status = :status
               AND (
                    (
                         :title IS NULL
                         AND :usersName IS NULL
                         AND :benefitName IS NULL
                         AND :benefitType IS NULL
                    )
                    OR (
                         :title IS NOT NULL
                         AND LOWER(listing.title)
                              LIKE LOWER(CONCAT('%', :title, '%'))
                    )
                    OR (
                         :usersName IS NOT NULL
                         AND LOWER(users.name)
                              LIKE LOWER(CONCAT('%', :usersName, '%'))
                    )
                    OR (
                         :benefitName IS NOT NULL
                         AND LOWER(benefit.benefitName)
                              LIKE LOWER(CONCAT('%', :benefitName, '%'))
                    )
                    OR (
                         :benefitType IS NOT NULL
                         AND LOWER(benefit.benefitType)
                              LIKE LOWER(CONCAT('%', :benefitType, '%'))
                    )
               )
               """)
     List<ExchangeListingBean> searchActiveListings(
               @Param("status") String status,
               @Param("title") String title,
               @Param("usersName") String usersName,
               @Param("benefitName") String benefitName,
               @Param("benefitType") String benefitType);

}
