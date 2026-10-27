package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;

/**
 * 持有紀錄（收藏庫）回傳資料。
 */
public record UseBenefitResponse(
        Integer useBenefitId,
        Integer benefitId,
        String benefitName,
        String movieName,
        String benefitType,
        String benefitStatus,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String officialImageUrl,
        List<UseBenefitPhotoResponse> photos,
        LocalDateTime createdAt) {

    /** 收藏庫搜尋電影名稱、卡片顯示電影用，查不到電影時是null */
    public static UseBenefitResponse from(UseBenefitBean bean, String movieName) {
        BenefitBean benefit = bean.getBenefit();
        List<UseBenefitPhotoResponse> photos = bean.getPhotos() == null
                ? List.of()
                : bean.getPhotos().stream().map(UseBenefitPhotoResponse::from).toList();

        return new UseBenefitResponse(
                bean.getUseBenefitId(),
                benefit.getBenefitId(),
                benefit.getBenefitName(),
                movieName,
                benefit.getBenefitType(),
                benefit.getStatus(),
                benefit.getStartDate(),
                benefit.getEndDate(),
                benefit.getOfficialImageUrl(),
                photos,
                bean.getCreatedAt());
    }
}
