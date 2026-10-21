package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.domain.UseBenefitPhotoBean;
import com.collection.benefitsjourney.domain.UsersBean;

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

    // 刊登者會員名稱
    private String usersName;

    // 持有特典的基本資料
    private Integer benefitId;
    private String benefitName;
    private String benefitType;

    // 特典官方圖片網址
    private String officialImageUrl;

    // 持有特典的照片 URL 清單
    private List<String> useBenefitPhotoUrls = new ArrayList<>();

    public static ExchangeListingResponse from(ExchangeListingBean bean) {

        ExchangeListingResponse response = new ExchangeListingResponse();

        response.setExchangeListingId(bean.getExchangeListingId());

        response.setTitle(bean.getTitle());
        response.setDescription(bean.getDescription());
        response.setStatus(bean.getStatus());
        response.setCreatedAt(bean.getCreatedAt());
        response.setUpdatedAt(bean.getUpdatedAt());

        UseBenefitBean useBenefit = bean.getUseBenefitBean();

        if (useBenefit != null) {

            response.setUseBenefitId(useBenefit.getUseBenefitId());

            // 取得持有特典的會員名稱
            UsersBean usersBean = useBenefit.getUser();

            if (usersBean != null) {
                response.setUsersName(usersBean.getName());
            }

            // 取得特典 ID、名稱、類型及官方圖片網址
            BenefitBean benefit = useBenefit.getBenefit();

            if (benefit != null) {
                response.setBenefitId(benefit.getBenefitId());
                response.setBenefitName(benefit.getBenefitName());
                response.setBenefitType(benefit.getBenefitType());
                response.setOfficialImageUrl(benefit.getOfficialImageUrl());
            }

            // 取得持有特典的會員照片網址
            if (useBenefit.getPhotos() != null) {
                response.setUseBenefitPhotoUrls(
                        useBenefit.getPhotos().stream()
                                .map(UseBenefitPhotoBean::getUseBenefitPhotoUrl)
                                .filter(url -> url != null && !url.isBlank())
                                .toList());
            }
        }
        return response;
    }
}
