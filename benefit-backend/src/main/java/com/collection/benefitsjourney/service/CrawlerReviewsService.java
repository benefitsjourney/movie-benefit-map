package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CrawlerReviewBean;
import com.collection.benefitsjourney.dto.CrawlerReviewRequest;
import com.collection.benefitsjourney.dto.CrawlerReviewResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.CrawlerReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CrawlerReviewsRepository;

@Service
public class CrawlerReviewsService {
    private final CrawlerReviewsRepository crawlerReviewsRepository;

    public CrawlerReviewsService(CrawlerReviewsRepository crawlerReviewsRepository) {
        this.crawlerReviewsRepository = crawlerReviewsRepository;
    }

    /** 依條件查詢爬蟲審核 */
    public PageResponse<CrawlerReviewResponse> findAll(
            CinemaResourceType crawlerType,
            ReviewStatus reviewStatus,
            int page,
            int size,
            CrawlerReviewSort.SortBy orderBy,
            SortOrder sort) {

        Sort.Direction direction = sort == SortOrder.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort order = Sort.by(direction, orderBy.getProperty());
        if (orderBy != CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID) {
            order = order.and(Sort.by(
                    Sort.Direction.ASC,
                    TypedPropertyPath.path((CrawlerReviewBean review) -> review.getCrawlerReviewId())));
        }

        PageRequest pageable = PageRequest.of(page - 1, size, order);

        Page<CrawlerReviewResponse> result = crawlerReviewsRepository.findAllByFilters(
                crawlerType,
                reviewStatus,
                pageable)
                .map(CrawlerReviewResponse::from);

        return PageResponse.from(result);
    }

    /** 新增爬蟲審核 */
    public CrawlerReviewResponse createCrawlerReview(CrawlerReviewRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.crawlerType() == null) {
            throw badRequest("爬蟲類型不可為空");
        }

        if (request.entityId() == null) {
            throw badRequest("對應資料ID不可為空");
        }

        if (request.fieldName() == null
                || request.fieldName().isBlank()) {
            throw badRequest("欄位名稱不可為空");
        }

        boolean hasNonLockedReview = crawlerReviewsRepository
                .findByCrawlerTypeAndEntityIdAndFieldName(
                        request.crawlerType(),
                        request.entityId(),
                        request.fieldName())
                .stream()
                .anyMatch(existing -> existing.getReviewStatus() != ReviewStatus.LOCKED);

        if (hasNonLockedReview) {
            throw badRequest("已存在相同資料的爬蟲審核，且審查狀態不是鎖定同步，無法新增");
        }

        CrawlerReviewBean review = new CrawlerReviewBean();

        review.setCrawlerType(request.crawlerType());
        review.setEntityId(request.entityId());
        review.setFieldName(request.fieldName());
        review.setCrawlerValue(request.crawlerValue());
        review.setPreviousValue(request.previousValue());
        review.setReviewStatus(
                request.reviewStatus() != null
                        ? request.reviewStatus()
                        : ReviewStatus.PENDING);
        review.setCrawledAt(request.crawledAt());
        review.setReviewedAt(request.reviewedAt());
        review.setUnlockedAt(request.unlockedAt());

        CrawlerReviewBean savedReview = crawlerReviewsRepository.save(review);

        return CrawlerReviewResponse.from(savedReview);
    }

    /** 修改爬蟲審核 */
    public CrawlerReviewResponse updateCrawlerReview(Integer id, CrawlerReviewRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.fieldName() != null
                && request.fieldName().isBlank()) {
            throw badRequest("欄位名稱不可為空");
        }

        if (request.reviewStatus() == ReviewStatus.LOCKED) {
            throw badRequest("已解鎖的審核無法修改");
        }

        CrawlerReviewBean review = crawlerReviewsRepository.findById(id)
                .orElseThrow(() -> notFound("找不到爬蟲審核 ID：" + id));

        if (request.crawlerType() != null) {
            review.setCrawlerType(request.crawlerType());
        }

        if (request.entityId() != null) {
            review.setEntityId(request.entityId());
        }

        if (request.fieldName() != null) {
            review.setFieldName(request.fieldName());
        }

        if (request.crawlerValue() != null) {
            review.setCrawlerValue(request.crawlerValue());
        }

        if (request.previousValue() != null) {
            review.setPreviousValue(request.previousValue());
        }

        if (request.reviewStatus() != null) {
            review.setReviewStatus(request.reviewStatus());
        }

        if (request.crawledAt() != null) {
            review.setCrawledAt(request.crawledAt());
        }

        if (request.reviewedAt() != null) {
            review.setReviewedAt(request.reviewedAt());
        }

        if (request.unlockedAt() != null) {
            review.setUnlockedAt(request.unlockedAt());
        }

        CrawlerReviewBean savedReview = crawlerReviewsRepository.save(review);

        return CrawlerReviewResponse.from(savedReview);
    }

    /** 刪除爬蟲審核 */
    public void deleteCrawlerReview(Integer id) {
        if (!crawlerReviewsRepository.existsById(id)) {
            throw notFound("找不到爬蟲審核 ID：" + id);
        }

        crawlerReviewsRepository.deleteById(id);
    }
}
