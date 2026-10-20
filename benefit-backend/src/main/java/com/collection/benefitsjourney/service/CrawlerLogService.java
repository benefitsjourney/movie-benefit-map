package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.CrawlerLogBean;
import com.collection.benefitsjourney.dto.CrawlerLogRequest;
import com.collection.benefitsjourney.dto.CrawlerLogResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CrawlerLogSort;
import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CrawlerLogRepository;

@Service
public class CrawlerLogService {
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
}
