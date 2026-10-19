package com.collection.benefitsjourney.controller.CinemaMovie;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerReviewRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerReviewResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaMovieCrawlerReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.service.CinemaMovie.CinemaMovieCrawlerReviewsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/cinema-movie/crawler-reviews")
@Tag(name = "影城電影爬蟲審核")
public class CinemaMovieCrawlerReviewsController {
    private final CinemaMovieCrawlerReviewsService crawlerReviewsService;

    public CinemaMovieCrawlerReviewsController(CinemaMovieCrawlerReviewsService crawlerReviewsService) {
        this.crawlerReviewsService = crawlerReviewsService;
    }

    @GetMapping
    @Operation(summary = "取得爬蟲審核資料")
    public ResponseEntity<PageResponse<CinemaMovieCrawlerReviewResponse>> findAll(
            @Parameter(description = "爬蟲類型") @RequestParam(required = false) CinemaResourceType crawlerType,

            @Parameter(description = "審查狀態") @RequestParam(required = false) ReviewStatus reviewStatus,

            @Parameter(description = "排序欄位") @RequestParam(defaultValue = "CRAWLER_REVIEW_ID") CinemaMovieCrawlerReviewSort.SortBy orderBy,

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

    @PostMapping("")
    @Operation(summary = "新增爬蟲審核")
    public ResponseEntity<CinemaMovieCrawlerReviewResponse> createCrawlerReview(
            @Validated({ Default.class,
                    CinemaMovieCrawlerReviewRequest.OnCreate.class }) @RequestBody CinemaMovieCrawlerReviewRequest request) {

        return ResponseEntity.ok(crawlerReviewsService.createCrawlerReview(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改爬蟲審核")
    public ResponseEntity<CinemaMovieCrawlerReviewResponse> updateCrawlerReview(
            @PathVariable Integer id,
            @Valid @RequestBody CinemaMovieCrawlerReviewRequest request) {

        return ResponseEntity.ok(crawlerReviewsService.updateCrawlerReview(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除爬蟲審核")
    public ResponseEntity<Void> deleteCrawlerReview(@PathVariable Integer id) {
        crawlerReviewsService.deleteCrawlerReview(id);

        return ResponseEntity.noContent().build();
    }
}
