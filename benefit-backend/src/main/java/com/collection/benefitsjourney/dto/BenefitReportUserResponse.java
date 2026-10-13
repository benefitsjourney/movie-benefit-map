package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.BenefitReportBean;

/** 使用者查看自己的特典回報（不含userId、publicId等內部欄位） */
public record BenefitReportUserResponse(
        Integer benefitReportId,
        Integer cinemaId,
        Integer movieId,
        String benefitName,
        String customCinemaName,
        String customMovieName,
        String status,
        String officialImageUrl,
        String sourceUrl,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static BenefitReportUserResponse from(BenefitReportBean bean) {
        return new BenefitReportUserResponse(
                bean.getBenefitReportId(),
                bean.getCinema() != null ? bean.getCinema().getCinemaId() : null,
                bean.getMovie() != null ? bean.getMovie().getMovieId() : null,
                bean.getBenefitName(),
                bean.getCustomCinemaName(),
                bean.getCustomMovieName(),
                bean.getStatus(),
                bean.getOfficialImageUrl(),
                bean.getSourceUrl(),
                bean.getReviewNote(),
                bean.getCreatedAt(),
                bean.getUpdatedAt());
    }
}