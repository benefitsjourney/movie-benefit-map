
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
// 用於回傳交換收藏資料及對應的交換刊登詳細資料給前端。
public class ExchangeFavoriteResponse {

    // 收藏資料
    private Integer exchangeFavoriteId;
    private Integer exchangeListingId;
    private LocalDateTime createdAt;

    // 交換刊登資料
    private String title;
    private String description;
    private String status;
    // 刊登最後修改時間，對應 exchange_listing.updated_at
    private LocalDateTime updatedAt;

    // 新增：交換刊登者會員名稱
    private String usersName;

    // 持有特典資料
    private String benefitName;
    private String benefitType;

    // 使用者持有特典的照片 URL 清單
    private List<String> useBenefitPhotoUrls = new ArrayList<>();

    public static ExchangeFavoriteResponse from(ExchangeFavoriteBean bean) {

        ExchangeFavoriteResponse response = new ExchangeFavoriteResponse();

        // 1. 收藏本身的資料
        response.setExchangeFavoriteId(bean.getExchangeFavoriteId());
        response.setCreatedAt(bean.getCreatedAt());

        // 2. 取得對應的交換刊登
        ExchangeListingBean listing = bean.getExchangeListingBean();

        if (listing != null) {

            response.setExchangeListingId(listing.getExchangeListingId());
            response.setTitle(listing.getTitle());
            response.setDescription(listing.getDescription());
            response.setStatus(listing.getStatus());

            // 使用刊登最後修改時間，不是收藏建立時間
            response.setUpdatedAt(listing.getUpdatedAt());

            // 3. 取得持有特典及照片
            UseBenefitBean useBenefit = listing.getUseBenefitBean();

            if (useBenefit != null) {

                // 取得刊登者名稱
                UsersBean usersBean = useBenefit.getUser();

                if (usersBean != null) {
                    response.setUsersName(usersBean.getName());
                }

                // 4. 取得特典名稱及類型
                BenefitBean benefit = useBenefit.getBenefit();

                if (benefit != null) {
                    response.setBenefitName(benefit.getBenefitName());
                    response.setBenefitType(benefit.getBenefitType());
                }

                // 5. 取得使用者上傳的持有特典照片 URL
                List<UseBenefitPhotoBean> photos = useBenefit.getPhotos();

                if (photos != null) {
                    response.setUseBenefitPhotoUrls(
                            photos.stream()
                                    .map(UseBenefitPhotoBean::getUseBenefitPhotoUrl)
                                    .filter(url -> url != null && !url.isBlank())
                                    .toList());
                }
            }
        }

        // if (bean.getExchangeListingBean() != null) {
        // response.setExchangeListingId(bean.getExchangeListingBean().getExchangeListingId());
        // }

        // response.setCreatedAt(bean.getCreatedAt());

        return response;
    }
}