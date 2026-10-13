package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CrawlerReviewResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.CrawlerReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.service.CrawlerReviewsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/crawler-reviews")
@Tag(name = "爬蟲審核")
public class CrawlerReviewsController {
    private final CrawlerReviewsService crawlerReviewsService;

    public CrawlerReviewsController(CrawlerReviewsService crawlerReviewsService) {
        this.crawlerReviewsService = crawlerReviewsService;
    }

    @GetMapping
    @Operation(summary = "取得爬蟲審核資料")
    public ResponseEntity<PageResponse<CrawlerReviewResponse>> findAll(
            @Parameter(description = "爬蟲類型") @RequestParam(required = false) CinemaResourceType crawlerType,

            @Parameter(description = "審查狀態") @RequestParam(required = false) ReviewStatus reviewStatus,

            @Parameter(description = "排序欄位") @RequestParam(defaultValue = "CRAWLER_REVIEW_ID") CrawlerReviewSort.SortBy orderBy,

            @Parameter(description = "排序方向") @RequestParam(defaultValue = "ASC") SortOrder sort,

            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                crawlerReviewsService.findAll(
                        crawlerType,
                        reviewStatus,
                        page,
                        size,
                        orderBy,
                        sort));
    }
}
