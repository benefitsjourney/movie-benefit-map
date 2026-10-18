package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CrawlerReviewBean;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.ReviewStatus;

public record CrawlerReviewResponse(
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

    public static CrawlerReviewResponse from(CrawlerReviewBean review) {
        return new CrawlerReviewResponse(
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
