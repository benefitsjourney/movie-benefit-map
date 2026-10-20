package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CrawlerLogBean;
import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;

public record CrawlerLogResponse(
        Integer crawlerLogId,
        CrawlerType crawlerType,
        CrawlerStatus crawlerStatus,
        Integer fetchedCount,
        Integer createdCount,
        Integer updatedCount,
        Integer skippedCount,
        Integer pendingReviewCount,
        String message,
        LocalDateTime executedAt,
        LocalDateTime finishedAt) {

    public static CrawlerLogResponse from(CrawlerLogBean log) {
        return new CrawlerLogResponse(
                log.getCrawlerLogId(),
                log.getCrawlerType(),
                log.getCrawlerStatus(),
                log.getFetchedCount(),
                log.getCreatedCount(),
                log.getUpdatedCount(),
                log.getSkippedCount(),
                log.getPendingReviewCount(),
                log.getMessage(),
                log.getExecutedAt(),
                log.getFinishedAt());
    }
}
