package com.collection.benefitsjourney.dto.CinemaMovie;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CinemaMovieReviewBean;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.ReviewStatus;

public record CinemaMovieReviewResponse(
        Integer cinemaMovieReviewId,
        CinemaResourceType crawlerType,
        Integer entityId,
        String entityName,
        String fieldName,
        String crawlerValue,
        String previousValue,
        ReviewStatus reviewStatus,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CinemaMovieReviewResponse from(CinemaMovieReviewBean review, String entityName) {
        return new CinemaMovieReviewResponse(
                review.getCinemaMovieReviewId(),
                review.getCrawlerType(),
                review.getEntityId(),
                entityName,
                review.getFieldName(),
                review.getCrawlerValue(),
                review.getPreviousValue(),
                review.getReviewStatus(),
                review.getReviewedAt(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
