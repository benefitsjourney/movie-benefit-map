package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.BenefitReportBean;

/** 管理員審核用的特典回報資料（含userId、publicId、reviewNote等內部欄位） */
public record BenefitReportAdminResponse(
        Integer benefitReportId,
        Integer userId,
        Integer cinemaId,
        Integer movieId,
        Integer benefitId,
        String benefitName,
        String customCinemaName,
        String customMovieName,
        Boolean isMemberOnly,
        String benefitType,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String officialImageUrl,
        String publicId,
        String sourceUrl,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static BenefitReportAdminResponse from(BenefitReportBean bean) {
        return new BenefitReportAdminResponse(
                bean.getBenefitReportId(),
                bean.getUserId(),
                bean.getCinemaId(),
                bean.getMovieId(),
                bean.getBenefitId(),
                bean.getBenefitName(),
                bean.getCustomCinemaName(),
                bean.getCustomMovieName(),
                bean.getIsMemberOnly(),
                bean.getBenefitType(),
                bean.getStatus(),
                bean.getStartDate(),
                bean.getEndDate(),
                bean.getOfficialImageUrl(),
                bean.getPublicId(),
                bean.getSourceUrl(),
                bean.getReviewNote(),
                bean.getCreatedAt(),
                bean.getUpdatedAt());
    }
}
