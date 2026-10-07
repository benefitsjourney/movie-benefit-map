package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.BenefitBean;

/** 特典回傳資料（不含 publicId、reviewNote 等內部欄位） */
public record BenefitResponse(
        Integer benefitId,
        String benefitName,
        Boolean isMemberOnly,
        String benefitType,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String officialImageUrl,
        String sourceUrl) {

    public static BenefitResponse from(BenefitBean bean) {
        return new BenefitResponse(
                bean.getBenefitId(),
                bean.getBenefitName(),
                bean.getIsMemberOnly(),
                bean.getBenefitType(),
                bean.getStatus(),
                bean.getStartDate(),
                bean.getEndDate(),
                bean.getOfficialImageUrl(),
                bean.getSourceUrl());
    }
}