package com.collection.benefitsjourney.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.service.CinemaService;

import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/cinemas")
public class CinemaController {
    private CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping("")
    @Operation(summary = "取得所有影城資料")
    public ResponseEntity<List<CinemaBean>> findAll() {
        return ResponseEntity.ok(cinemaService.findAll());
    }

}
