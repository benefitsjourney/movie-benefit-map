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
        LocalDateTime updatedAt,
        // 爬蟲從原文判斷出的影城ID（逗號分隔），審核時預先勾選用
        String matchedCinemaIds,
        // 對應到的電影名稱（沒對應到就是null）
        String movieName) {

    public static BenefitReportAdminResponse from(BenefitReportBean bean) {
        return new BenefitReportAdminResponse(
                bean.getBenefitReportId(),
                bean.getUser() != null ? bean.getUser().getUsersId() : null,
                bean.getCinema() != null ? bean.getCinema().getCinemaId() : null,
                bean.getMovie() != null ? bean.getMovie().getMovieId() : null,
                bean.getBenefit() != null ? bean.getBenefit().getBenefitId() : null,
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
                bean.getUpdatedAt(),
                bean.getMatchedCinemaIds(),
                bean.getMovie() != null ? bean.getMovie().getMovieName() : null);
    }
}
