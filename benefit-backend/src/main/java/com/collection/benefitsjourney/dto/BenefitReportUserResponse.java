package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.BenefitReportBean;

/** 使用者查看自己的特典回報（不含userId、publicId等內部欄位） */
public record BenefitReportUserResponse(
        Integer benefitReportId,
        Integer cinemaId,
        String cinemaName,
        Integer movieId,
        String movieName,
        Integer benefitId,
        String benefitName,
        String customCinemaName,
        String customMovieName,
        String status,
        String officialImageUrl,
        String sourceUrl,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // 勾選的影城數量（「信義威秀 等3間」顯示用）
        int cinemaCount) {

    public static BenefitReportUserResponse from(BenefitReportBean bean) {
        return new BenefitReportUserResponse(
                bean.getBenefitReportId(),
                bean.getCinema() != null ? bean.getCinema().getCinemaId() : null,
                bean.getCinema() != null ? bean.getCinema().getCinemaName() : null,
                bean.getMovie() != null ? bean.getMovie().getMovieId() : null,
                bean.getMovie() != null ? bean.getMovie().getMovieName() : null,
                bean.getBenefit() != null ? bean.getBenefit().getBenefitId() : null,
                bean.getBenefitName(),
                bean.getCustomCinemaName(),
                bean.getCustomMovieName(),
                bean.getStatus(),
                bean.getOfficialImageUrl(),
                bean.getSourceUrl(),
                bean.getReviewNote(),
                bean.getCreatedAt(),
                bean.getUpdatedAt(),
                countCinemas(bean));
    }

    /** matchedCinemaIds是"1,2,3"，數有幾間；沒有的話看cinema_id有沒有值 */
    private static int countCinemas(BenefitReportBean bean) {
        String ids = bean.getMatchedCinemaIds();
        if (ids != null && !ids.isBlank()) {
            return (int) java.util.Arrays.stream(ids.split(","))
                    .filter(s -> !s.isBlank())
                    .count();
        }
        return bean.getCinema() != null ? 1 : 0;
    }
}