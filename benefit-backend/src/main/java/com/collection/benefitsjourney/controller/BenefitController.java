package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.BenefitResponse;
import com.collection.benefitsjourney.service.BenefitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "特典", description = "特典查詢")
public class BenefitController {

    private final BenefitService benefitService;

    @Operation(summary = "查詢已上架特典清單", description = "只查status=APPROVED的特典")
    @GetMapping("/benefits")
    public List<BenefitResponse> getBenefits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return benefitService.getApprovedBenefits(page, size);
    }

    @Operation(summary = "查詢單一特典詳情")
    @GetMapping("/benefits/{benefitId}")
    public BenefitResponse getBenefit(@PathVariable Integer benefitId) {
        return benefitService.getBenefit(benefitId);
    }

    @Operation(summary = "查某部電影有哪些特典", description = "status可選APPROVED／EXPIRED，預設APPROVED")
    @GetMapping("/movies/{movieId}/benefits")
    public List<BenefitResponse> getBenefitsByMovie(
            @PathVariable Integer movieId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return benefitService.getBenefitsByMovie(movieId, status, page, size);
    }
}