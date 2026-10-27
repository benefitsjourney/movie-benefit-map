package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.ExchangeFavoriteBean;
import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.domain.UseBenefitPhotoBean;
import com.collection.benefitsjourney.domain.UsersBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於回傳會員收藏的交換刊登資料給前端
public class ExchangeFavoriteResponse {

    // 收藏紀錄資料
    private Integer exchangeFavoriteId;
    private Integer exchangeListingId;
    private LocalDateTime createdAt;

    // 交換刊登資料
    private String title;
    private String description;
    private String status;

    // 交換刊登建立及更新時間
    private LocalDateTime listingCreatedAt;
    private LocalDateTime updatedAt;

    // 刊登者會員資料
    private Integer usersId;
    private String usersName;

    // 持有特典基本資料
    private Integer useBenefitId;
    private Integer benefitId;
    private String benefitName;
    private String benefitType;

    // 特典官方圖片網址
    private String officialImageUrl;

    // 會員持有特典的照片 URL 清單
    private List<String> useBenefitPhotoUrls = new ArrayList<>();

    public static ExchangeFavoriteResponse from(ExchangeFavoriteBean bean) {

        ExchangeFavoriteResponse response = new ExchangeFavoriteResponse();

        if (bean == null) {
            return response;
        }

        // 收藏紀錄資料
        response.setExchangeFavoriteId(bean.getExchangeFavoriteId());

        if (bean.getCreatedAt() != null) {
            response.setCreatedAt(bean.getCreatedAt());
        }

        ExchangeListingBean listing = bean.getExchangeListingBean();

        if (listing == null) {
            return response;
        }

        // 交換刊登資料
        response.setExchangeListingId(listing.getExchangeListingId());
        response.setTitle(listing.getTitle());
        response.setDescription(listing.getDescription());
        response.setStatus(listing.getStatus());
        response.setListingCreatedAt(listing.getCreatedAt());
        response.setUpdatedAt(listing.getUpdatedAt());

        UseBenefitBean useBenefit = listing.getUseBenefitBean();

        if (useBenefit == null) {
            return response;
        }

        response.setUseBenefitId(useBenefit.getUseBenefitId());

        // 刊登者會員資料
        UsersBean usersBean = useBenefit.getUser();

        if (usersBean != null) {
            response.setUsersId(usersBean.getUsersId());
            response.setUsersName(usersBean.getName());
        }

        // 特典資料及官方圖片
        BenefitBean benefit = useBenefit.getBenefit();

        if (benefit != null) {
            response.setBenefitId(benefit.getBenefitId());
            response.setBenefitName(benefit.getBenefitName());
            response.setBenefitType(benefit.getBenefitType());
            response.setOfficialImageUrl(benefit.getOfficialImageUrl());
        }

        // 會員持有特典照片
        if (useBenefit.getPhotos() != null) {
            response.setUseBenefitPhotoUrls(
                    useBenefit.getPhotos().stream()
                            .map(UseBenefitPhotoBean::getUseBenefitPhotoUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .distinct()
                            .limit(5)
                            .toList());
        }

        return response;
    }
}