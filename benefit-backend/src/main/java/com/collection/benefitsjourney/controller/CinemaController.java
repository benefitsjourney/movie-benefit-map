package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CinemaCreateRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.service.CinemaService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/cinemas")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping("")
    @Operation(summary = "取得所有影城資料")
    public ResponseEntity<List<CinemaResponse>> findAll() {
        return ResponseEntity.ok(cinemaService.findAll());
    }

    @PostMapping("")
    @Operation(summary = "新增影城")
    public ResponseEntity<CinemaResponse> createCinema(
            @RequestBody CinemaCreateRequest request) {

        return ResponseEntity.ok(cinemaService.createCinema(request));
    }
}