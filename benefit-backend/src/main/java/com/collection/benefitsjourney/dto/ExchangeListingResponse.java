package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.ExchangeListingBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於回傳交換刊登資料給前端
public class ExchangeListingResponse {
    private Integer exchangeListingId;
    private Integer useBenefitId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExchangeListingResponse from(ExchangeListingBean bean) {

        ExchangeListingResponse response = new ExchangeListingResponse();

        response.setExchangeListingId(bean.getExchangeListingId());

        if (bean.getUseBenefitBean() != null) {
            response.setUseBenefitId(bean.getUseBenefitBean().getUseBenefitId());
        }

        response.setTitle(bean.getTitle());
        response.setDescription(bean.getDescription());
        response.setStatus(bean.getStatus());
        response.setCreatedAt(bean.getCreatedAt());
        response.setUpdatedAt(bean.getUpdatedAt());

        return response;
    }
}
