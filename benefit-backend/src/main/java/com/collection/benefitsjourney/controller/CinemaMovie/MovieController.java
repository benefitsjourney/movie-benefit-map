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

import com.collection.benefitsjourney.dto.CinemaMovie.MovieRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.MovieSort;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.service.CinemaMovie.MovieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/movies")
@Tag(name = "電影")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    @Operation(summary = "取得所有電影資料")
    public ResponseEntity<PageResponse<MovieResponse>> findAll(
            @Parameter(description = "電影名稱模糊搜尋關鍵字") @RequestParam(required = false) String keyword,

            @Parameter(description = "電影是否顯示") @RequestParam(required = false) Boolean isVisible,

            @Parameter(description = "排序欄位") @RequestParam(defaultValue = "MOVIE_ID") MovieSort.SortBy orderBy,

            @Parameter(description = "排序方向") @RequestParam(defaultValue = "ASC") SortOrder sort,

            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "24") int size) {

        return ResponseEntity.ok(
                movieService.findAll(
                        keyword,
                        isVisible,
                        page,
                        size,
                        orderBy,
                        sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "取得指定id電影資料")
    public ResponseEntity<MovieResponse> findById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(movieService.findById(id));
    }

    @PostMapping("")
    @Operation(summary = "新增電影")
    public ResponseEntity<MovieResponse> createMovie(
            @Validated({ Default.class, MovieRequest.OnCreate.class }) @RequestBody MovieRequest request) {

        return ResponseEntity.ok(movieService.createMovie(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改電影")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Integer id,
            @Valid @RequestBody MovieRequest request) {

        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }
}
