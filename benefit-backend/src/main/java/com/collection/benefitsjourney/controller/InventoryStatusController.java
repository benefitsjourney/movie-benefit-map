package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.InventoryStatusResponse;
import com.collection.benefitsjourney.service.InventoryStatusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "庫存查詢", description = "給地圖頁與特典庫存查詢頁使用")
public class InventoryStatusController {

    private final InventoryStatusService inventoryStatusService;

    @GetMapping("/benefits/{benefitId}/cinemas/status")
    @Operation(summary = "查某特典在哪些影城有庫存")
    public ResponseEntity<List<InventoryStatusResponse>> findByBenefit(
            @PathVariable Integer benefitId) {
        return ResponseEntity.ok(inventoryStatusService.findByBenefit(benefitId));
    }

    @GetMapping("/cinemas/{cinemaId}/benefits/status")
    @Operation(summary = "查某影城的特典庫存狀態", description = "只列出上架中的特典")
    public ResponseEntity<List<InventoryStatusResponse>> findByCinema(
            @PathVariable Integer cinemaId) {
        return ResponseEntity.ok(inventoryStatusService.findByCinema(cinemaId));
    }
}
