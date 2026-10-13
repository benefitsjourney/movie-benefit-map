package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.ExchangeFavoriteBean;

public interface ExchangeFavoriteRepository extends JpaRepository<ExchangeFavoriteBean, Integer> {

        // 檢查會員是否已經收藏這筆交換刊登
        boolean existsByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(Integer usersId,
                        Integer exchangeListingId);

        // 查詢會員對指定交換刊登的收藏
        Optional<ExchangeFavoriteBean> findByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(Integer usersId,
                        Integer exchangeListingId);

        // 查詢會員自己的所有交換收藏
        List<ExchangeFavoriteBean> findByUsersBean_UsersId(Integer usersId);
}
