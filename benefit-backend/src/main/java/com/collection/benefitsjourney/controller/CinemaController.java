package com.collection.benefitsjourney.controller;

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

import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.dto.NearbyCinemaResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaSort;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.service.CinemaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/cinemas")
@Tag(name = "影城")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping
    @Operation(summary = "取得所有影城資料")
    public ResponseEntity<PageResponse<CinemaResponse>> findAll(
            @Parameter(description = "影城名稱模糊搜尋關鍵字") @RequestParam(required = false) String keyword,

            @Parameter(description = "縣市篩選") @RequestParam(required = false) County county,

            @Parameter(description = "是否仍在營業") @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "是否在前台顯示") @RequestParam(required = false) Boolean isVisible,

            @Parameter(description = "排序欄位") @RequestParam(defaultValue = "CINEMA_ID") CinemaSort.SortBy orderBy,

            @Parameter(description = "排序方向") @RequestParam(defaultValue = "ASC") SortOrder sort,

            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                cinemaService.findAll(
                        keyword,
                        county,
                        isActive,
                        isVisible,
                        page,
                        size,
                        orderBy,
                        sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "取得指定id影城資料")
    public ResponseEntity<CinemaResponse> findById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(cinemaService.findById(id));
    }

    @PostMapping("")
    @Operation(summary = "新增影城")
    public ResponseEntity<CinemaResponse> createCinema(
            @Validated({ Default.class, CinemaRequest.OnCreate.class }) @RequestBody CinemaRequest request) {

        return ResponseEntity.ok(cinemaService.createCinema(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改影城")
    public ResponseEntity<CinemaResponse> updateCinema(
            @PathVariable Integer id,
            @Valid @RequestBody CinemaRequest request) {

        return ResponseEntity.ok(cinemaService.updateCinema(id, request));
    }

    /** 取得指定座標附近的影城 */
    @GetMapping("/nearby")
    @Operation(summary = "取得指定座標附近的影城")
    public ResponseEntity<NearbyCinemaResponse> findNearby(
            @RequestParam(defaultValue = "25.0480") double latitude,
            @RequestParam(defaultValue = "121.5170") double longitude,
            @RequestParam(defaultValue = "20000") double radius) {

        return ResponseEntity.ok(
                cinemaService.findNearby(latitude, longitude, radius));
    }
}