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
        String benefitType,
        String benefitStatus,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String officialImageUrl,
        List<UseBenefitPhotoResponse> photos,
        LocalDateTime createdAt) {

    public static UseBenefitResponse from(UseBenefitBean bean) {
        BenefitBean benefit = bean.getBenefit();
        List<UseBenefitPhotoResponse> photos = bean.getPhotos() == null
                ? List.of()
                : bean.getPhotos().stream().map(UseBenefitPhotoResponse::from).toList();

        return new UseBenefitResponse(
                bean.getUseBenefitId(),
                benefit.getBenefitId(),
                benefit.getBenefitName(),
                benefit.getBenefitType(),
                benefit.getStatus(),
                benefit.getStartDate(),
                benefit.getEndDate(),
                benefit.getOfficialImageUrl(),
                photos,
                bean.getCreatedAt());
    }
}