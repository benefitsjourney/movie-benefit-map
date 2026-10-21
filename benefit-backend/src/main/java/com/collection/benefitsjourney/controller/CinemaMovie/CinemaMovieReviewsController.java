package com.collection.benefitsjourney.controller.CinemaMovie;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaMovieReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.service.CinemaMovie.CinemaMovieReviewsService;
import com.collection.benefitsjourney.service.UsersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/cinema-movie/reviews")
@Tag(name = "影城電影爬蟲審核")
public class CinemaMovieReviewsController {
    private final CinemaMovieReviewsService cinemaMovieReviewsService;
    private final UsersService usersService;

    public CinemaMovieReviewsController(
            CinemaMovieReviewsService cinemaMovieReviewsService,
            UsersService usersService) {
        this.cinemaMovieReviewsService = cinemaMovieReviewsService;
        this.usersService = usersService;
    }

    @GetMapping
    @Operation(summary = "取得爬蟲審核資料")
    public ResponseEntity<PageResponse<CinemaMovieReviewResponse>> findAll(
            @Parameter(description = "爬蟲類型") @RequestParam(required = false) CinemaResourceType crawlerType,

            @Parameter(description = "審查狀態") @RequestParam(required = false) ReviewStatus reviewStatus,

            @Parameter(description = "排序欄位") @RequestParam(defaultValue = "CINEMA_MOVIE_REVIEW_ID") CinemaMovieReviewSort.SortBy orderBy,

            @Parameter(description = "排序方向") @RequestParam(defaultValue = "ASC") SortOrder sort,

            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        ExceptionUtils.requireAdmin(httpRequest, usersService);

        return ResponseEntity.ok(
                cinemaMovieReviewsService.findAll(
                        crawlerType,
                        reviewStatus,
                        page,
                        size,
                        orderBy,
                        sort));
    }

    @PostMapping("")
    @Operation(summary = "新增爬蟲審核")
    public ResponseEntity<CinemaMovieReviewResponse> createCinemaMovieReview(
            @Validated({ Default.class,
                    CinemaMovieReviewRequest.OnCreate.class }) @RequestBody CinemaMovieReviewRequest request,
            HttpServletRequest httpRequest) {

        ExceptionUtils.requireAdmin(httpRequest, usersService);
        return ResponseEntity.ok(cinemaMovieReviewsService.createCinemaMovieReview(request));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "審核通過", description = "僅待審核可通過。通過後爬蟲值寫入正式影城或電影；已審核則回傳你已審核過")
    public ResponseEntity<CinemaMovieReviewResponse> approveCinemaMovieReview(
            @PathVariable Integer id,
            HttpServletRequest httpRequest) {
        ExceptionUtils.requireAdmin(httpRequest, usersService);
        return ResponseEntity.ok(cinemaMovieReviewsService.approveCinemaMovieReview(id));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "審核拒絕", description = "僅待審核可拒絕。只把狀態改成 REJECTED，不寫入正式影城或電影")
    public ResponseEntity<CinemaMovieReviewResponse> rejectCinemaMovieReview(
            @PathVariable Integer id,
            HttpServletRequest httpRequest) {
        ExceptionUtils.requireAdmin(httpRequest, usersService);
        return ResponseEntity.ok(cinemaMovieReviewsService.rejectCinemaMovieReview(id));
    }
}
