package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;

import java.time.LocalDateTime;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

import com.collection.benefitsjourney.domain.CrawlerLogBean;
import com.collection.benefitsjourney.dto.CrawlerLogRequest;
import com.collection.benefitsjourney.dto.CrawlerLogResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CrawlerLogSort;
import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CrawlerLogRepository;

@Slf4j
@Service
public class CrawlerLogService {
    private static final int MESSAGE_LIMIT = 1024;
    private final CrawlerLogRepository crawlerLogRepository;

    public CrawlerLogService(CrawlerLogRepository crawlerLogRepository) {
        this.crawlerLogRepository = crawlerLogRepository;
    }

    /** 依條件查詢爬蟲紀錄 */
    public PageResponse<CrawlerLogResponse> findAll(
            CrawlerType crawlerType,
            CrawlerStatus crawlerStatus,
            int page,
            int size,
            CrawlerLogSort.SortBy orderBy,
            SortOrder sort) {

        Sort.Direction direction = sort == SortOrder.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort order = Sort.by(direction, orderBy.getProperty());

        if (orderBy != CrawlerLogSort.SortBy.CRAWLER_LOG_ID) {
            order = order.and(Sort.by(
                    Sort.Direction.ASC,
                    TypedPropertyPath.path((CrawlerLogBean log) -> log.getCrawlerLogId())));
        }

        PageRequest pageable = PageRequest.of(page - 1, size, order);

        Page<CrawlerLogResponse> result = crawlerLogRepository.findAllByFilters(
                crawlerType,
                crawlerStatus,
                pageable)
                .map(CrawlerLogResponse::from);

        return PageResponse.from(result);
    }

    /** 新增一筆爬蟲執行紀錄 */
    @Transactional
    public CrawlerLogResponse createCrawlerLog(CrawlerLogRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }
        if (request.crawlerType() == null) {
            throw badRequest("爬蟲類型不可為空");
        }
        if (request.crawlerStatus() == null) {
            throw badRequest("執行狀態不可為空");
        }
        if (request.fetchedCount() == null) {
            throw badRequest("抓取筆數不可為空");
        }
        if (request.createdCount() == null) {
            throw badRequest("新增筆數不可為空");
        }
        if (request.updatedCount() == null) {
            throw badRequest("更新筆數不可為空");
        }
        if (request.skippedCount() == null) {
            throw badRequest("略過筆數不可為空");
        }
        if (request.pendingReviewCount() == null) {
            throw badRequest("納入審核筆數不可為空");
        }
        if (request.executedAt() == null) {
            throw badRequest("執行時間不可為空");
        }
        if (request.finishedAt() == null) {
            throw badRequest("結束時間不可為空");
        }

        CrawlerLogBean log = new CrawlerLogBean();
        log.setCrawlerType(request.crawlerType());
        log.setCrawlerStatus(request.crawlerStatus());
        log.setFetchedCount(request.fetchedCount());
        log.setCreatedCount(request.createdCount());
        log.setUpdatedCount(request.updatedCount());
        log.setSkippedCount(request.skippedCount());
        log.setPendingReviewCount(request.pendingReviewCount());
        log.setMessage(request.message());
        log.setExecutedAt(request.executedAt().withNano(0));
        log.setFinishedAt(request.finishedAt().withNano(0));
        return CrawlerLogResponse.from(crawlerLogRepository.save(log));
    }

    /** 爬蟲結束後寫入紀錄。寫入失敗不影響爬蟲結果。 */
    public void recordRun(
            CrawlerType crawlerType,
            CrawlerStatus crawlerStatus,
            int fetchedCount,
            int createdCount,
            int updatedCount,
            int skippedCount,
            int pendingReviewCount,
            String message,
            LocalDateTime executedAt,
            LocalDateTime finishedAt) {
        try {
            createCrawlerLog(new CrawlerLogRequest(
                    crawlerType,
                    crawlerStatus,
                    fetchedCount,
                    createdCount,
                    updatedCount,
                    skippedCount,
                    pendingReviewCount,
                    limitMessage(message),
                    executedAt,
                    finishedAt));
        } catch (RuntimeException e) {
            log.warn("爬蟲紀錄寫入失敗：{}", crawlerType, e);
        }
    }

    public static String failureMessage(Throwable error) {
        String message = null;
        if (error instanceof ResponseStatusException response && response.getReason() != null) {
            message = response.getReason();
        } else if (error != null) {
            message = error.getMessage();
        }
        if (message == null || message.isBlank()) {
            return "爬蟲執行失敗";
        }
        return message.strip();
    }

    public static String withNote(String message, String note) {
        if (note == null || note.isBlank()) {
            return message;
        }
        if (message == null || message.isBlank()) {
            return note;
        }
        return (message.endsWith("。") ? message : message + "。") + note;
    }

    private static String limitMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String trimmed = message.strip();
        return trimmed.length() <= MESSAGE_LIMIT ? trimmed : trimmed.substring(0, MESSAGE_LIMIT);
    }
}
