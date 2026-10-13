package com.collection.benefitsjourney.service;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CrawlerReviewBean;
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
}
