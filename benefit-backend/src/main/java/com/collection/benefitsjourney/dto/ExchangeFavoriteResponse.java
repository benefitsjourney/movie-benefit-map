
package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.ExchangeFavoriteBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於回傳交換收藏資料給前端。
public class ExchangeFavoriteResponse {

    private Integer exchangeFavoriteId;
    private Integer exchangeListingId;
    private LocalDateTime createdAt;

    public static ExchangeFavoriteResponse from(ExchangeFavoriteBean bean) {

        ExchangeFavoriteResponse response = new ExchangeFavoriteResponse();

        response.setExchangeFavoriteId(bean.getExchangeFavoriteId());

        if (bean.getExchangeListingBean() != null) {
            response.setExchangeListingId(bean.getExchangeListingBean().getExchangeListingId());
        }

        response.setCreatedAt(bean.getCreatedAt());

        return response;
    }
}