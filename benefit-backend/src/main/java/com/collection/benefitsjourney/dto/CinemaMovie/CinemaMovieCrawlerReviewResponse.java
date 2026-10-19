package com.collection.benefitsjourney.dto.CinemaMovie;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CinemaMovieCrawlerReviewBean;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.ReviewStatus;

public record CinemaMovieCrawlerReviewResponse(
        Integer crawlerReviewId,
        CinemaResourceType crawlerType,
        Integer entityId,
        String fieldName,
        String crawlerValue,
        String previousValue,
        ReviewStatus reviewStatus,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CinemaMovieCrawlerReviewResponse from(CinemaMovieCrawlerReviewBean review) {
        return new CinemaMovieCrawlerReviewResponse(
                review.getCrawlerReviewId(),
                review.getCrawlerType(),
                review.getEntityId(),
                review.getFieldName(),
                review.getCrawlerValue(),
                review.getPreviousValue(),
                review.getReviewStatus(),
                review.getReviewedAt(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
