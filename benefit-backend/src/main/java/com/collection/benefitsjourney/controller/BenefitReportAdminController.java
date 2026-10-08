package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.BenefitReportAdminResponse;
import com.collection.benefitsjourney.dto.BenefitReportRejectRequest;
import com.collection.benefitsjourney.dto.BenefitReportUpdateRequest;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitReportStatus;
import com.collection.benefitsjourney.service.BenefitReportAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// TODO: 管理員權限（等登入與權限機制完成後，限制只有ADMIN可呼叫）
@RestController
@RequestMapping("/benefit-reports")
@RequiredArgsConstructor
@Tag(name = "特典回報審核（管理員）")
public class BenefitReportAdminController {

    private final BenefitReportAdminService benefitReportAdminService;

    @GetMapping
    @Operation(summary = "查詢特典回報清單", description = "預設查PENDING（待審核），先送先審")
    public ResponseEntity<PageResponse<BenefitReportAdminResponse>> findByStatus(
            @Parameter(description = "審核狀態") @RequestParam(defaultValue = "PENDING") BenefitReportStatus status,
            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(benefitReportAdminService.findByStatus(status, page, size));
    }

    @GetMapping("/{benefitReportId}")
    @Operation(summary = "查詢單一特典回報")
    public ResponseEntity<BenefitReportAdminResponse> findById(
            @PathVariable Integer benefitReportId) {

        return ResponseEntity.ok(benefitReportAdminService.findById(benefitReportId));
    }

    @PatchMapping("/{benefitReportId}")
    @Operation(summary = "修改待審核的特典回報", description = "只有PENDING可修改，只更新有帶的欄位，不可修改status")
    public ResponseEntity<BenefitReportAdminResponse> update(
            @PathVariable Integer benefitReportId,
            @Validated @RequestBody BenefitReportUpdateRequest request) {

        return ResponseEntity.ok(benefitReportAdminService.update(benefitReportId, request));
    }

    @PatchMapping("/{benefitReportId}/reject")
    @Operation(summary = "駁回待審核的特典回報", description = "只有PENDING可駁回，reviewNote為駁回理由（選填），會給使用者查看")
    public ResponseEntity<BenefitReportAdminResponse> reject(
            @PathVariable Integer benefitReportId,
            @Validated @RequestBody(required = false) BenefitReportRejectRequest request) {

        return ResponseEntity.ok(benefitReportAdminService.reject(benefitReportId, request));
    }
}
