package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.BenefitBean;

/** 特典列表用（比BenefitResponse多了電影資訊） */
public record BenefitListResponse(
        Integer benefitId,
        String benefitName,
        Boolean isMemberOnly,
        String benefitType,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String officialImageUrl,
        Integer movieId,
        String movieName) {

    public static BenefitListResponse from(BenefitBean bean, Integer movieId, String movieName) {
        return new BenefitListResponse(
                bean.getBenefitId(),
                bean.getBenefitName(),
                bean.getIsMemberOnly(),
                bean.getBenefitType(),
                bean.getStatus(),
                bean.getStartDate(),
                bean.getEndDate(),
                bean.getOfficialImageUrl(),
                movieId,
                movieName);
    }
}