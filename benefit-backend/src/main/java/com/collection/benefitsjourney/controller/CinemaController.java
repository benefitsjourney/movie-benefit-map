package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.service.CinemaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cinemas")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping
    @Operation(summary = "取得所有影城資料")
    public ResponseEntity<List<CinemaResponse>> findAll(
            @Parameter(description = "影城名稱模糊搜尋關鍵字，例如：威秀") @RequestParam(required = false) String keyword,
            @Parameter(description = "縣市篩選") @RequestParam(required = false) County county,
            @Parameter(description = "是否仍在營業") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "是否在前台顯示") @RequestParam(required = false) Boolean isVisible) {
        return ResponseEntity.ok(
                cinemaService.findAll(
                        keyword,
                        county,
                        isActive,
                        isVisible));
    }

    @PostMapping("")
    @Operation(summary = "新增影城")
    public ResponseEntity<CinemaResponse> createCinema(
            @Valid @RequestBody CinemaRequest request) {

        return ResponseEntity.ok(cinemaService.createCinema(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改影城")
    public ResponseEntity<CinemaResponse> updateCinema(
            @PathVariable Integer id,
            @Valid @RequestBody CinemaRequest request) {

        return ResponseEntity.ok(cinemaService.updateCinema(id, request));
    }
}