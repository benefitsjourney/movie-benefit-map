package com.collection.benefitsjourney.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.InventoryReportCreateRequest;
import com.collection.benefitsjourney.dto.InventoryReportResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.service.InventoryReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory-reports")
@RequiredArgsConstructor
@Tag(name = "庫存回報（會員）", description = "新增特典庫存、查看自己的回報")
public class InventoryReportController {

    private final InventoryReportService inventoryReportService;

    @PostMapping
    @Operation(summary = "新增庫存回報",
            description = "帶cinemaMovieBenefitId與status（IN_STOCK／LOW_STOCK／EXHAUSTED）"
                    + "同一個影城特典需間隔15分鐘才能再回報，冷卻時間回429")
    public ResponseEntity<InventoryReportResponse> create(
            @Validated @RequestBody InventoryReportCreateRequest request,
            HttpServletRequest httpRequest) {

    	Integer usersId = getUsersId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryReportService.create(usersId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "查看自己的庫存回報", description = "新到舊")
    public ResponseEntity<PageResponse<InventoryReportResponse>> findMyReports(
            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

    	Integer usersId = getUsersId(httpRequest);
        return ResponseEntity.ok(inventoryReportService.findMyReports(usersId, page, size));
    }
    
    private Integer getUsersId(HttpServletRequest request) {
        Object value = request.getAttribute("usersId");

        if (!(value instanceof Integer)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "尚未取得登入會員資料");
        }

        return (Integer) value;
    }
}
