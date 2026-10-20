package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.BenefitListResponse;
import com.collection.benefitsjourney.dto.BenefitResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.BenefitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "特典", description = "特典查詢")
public class BenefitController {

    private final BenefitService benefitService;

    @Operation(summary = "查詢已上架特典清單",
            description = "只查status=APPROVED的特典。keyword會同時搜尋特典名稱與電影名稱")
    @GetMapping("/benefits")
    public ResponseEntity<PageResponse<BenefitListResponse>> getBenefits(
            @Parameter(description = "關鍵字（特典名稱或電影名稱）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "特典類型")
            @RequestParam(required = false) BenefitType benefitType,
    		@Parameter(description = "頁碼")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(benefitService.getApprovedBenefits(keyword, benefitType, page, size));
    }

    @Operation(summary = "查詢單一特典詳情")
    @GetMapping("/benefits/{benefitId}")
    public ResponseEntity<BenefitResponse> getBenefit(@PathVariable Integer benefitId) {
        return ResponseEntity.ok(benefitService.getBenefit(benefitId));
    }

    @Operation(summary = "查某部電影有哪些特典", description = "status可選APPROVED／EXPIRED，預設APPROVED")
    @GetMapping("/movies/{movieId}/benefits")
    public ResponseEntity<PageResponse<BenefitResponse>> getBenefitsByMovie(
            @PathVariable Integer movieId,
            @Parameter(description = "特典狀態")
            @RequestParam(defaultValue = "APPROVED") BenefitStatus status,
            @Parameter(description = "頁碼")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(benefitService.getBenefitsByMovie(movieId, status, page, size));
    }
}