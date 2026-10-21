package com.collection.benefitsjourney.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.BenefitListResponse;
import com.collection.benefitsjourney.dto.BenefitResponse;
import com.collection.benefitsjourney.dto.BenefitStatusRequest;
import com.collection.benefitsjourney.dto.BenefitUpdateRequest;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.BenefitAdminService;
import com.collection.benefitsjourney.service.BenefitService;
import com.collection.benefitsjourney.service.UsersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/benefits")
@RequiredArgsConstructor
@Tag(name = "特典管理（Admin）", description = "查詢、修改、上下架、刪除正式特典")
public class BenefitAdminController {

    private final BenefitAdminService benefitAdminService;
    private final UsersService usersService;
    private final BenefitService benefitService;

    /** 非管理員回403 */
    private void requireAdmin(HttpServletRequest request) {
        Integer usersId = (Integer) request.getAttribute("usersId");
        UsersBean user = usersService.getUserById(usersId);
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您沒有權限執行此操作");
        }
    }
    
    @GetMapping("/admin")
    @Operation(summary = "查詢特典清單（後台）",
            description = "可看已上架與已下架。status不帶＝全部；keyword會同時搜尋特典名稱與電影名稱")
    public ResponseEntity<PageResponse<BenefitListResponse>> findAll(
            @Parameter(description = "特典狀態（不帶＝全部）") @RequestParam(required = false) BenefitStatus status,
            @Parameter(description = "關鍵字（特典名稱或電影名稱）") @RequestParam(required = false) String keyword,
            @Parameter(description = "特典類型") @RequestParam(required = false) BenefitType benefitType,
            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        return ResponseEntity.ok(benefitService.getBenefitsForAdmin(status, keyword, benefitType, page, size));
    }

    @PatchMapping("/{benefitId}")
    @Operation(summary = "修改特典（正式資料）",
            description = "只更新有帶的欄位，不可修改status（上下架用 /status）")
    public ResponseEntity<BenefitResponse> update(
            @PathVariable Integer benefitId,
            @Validated @RequestBody BenefitUpdateRequest request,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        return ResponseEntity.ok(benefitAdminService.update(benefitId, request));
    }

    @PatchMapping("/{benefitId}/status")
    @Operation(summary = "上下架特典",
            description = "status可選APPROVED（上架）／EXPIRED（下架）。結束日已過的特典要先修改結束日才能重新上架")
    public ResponseEntity<BenefitResponse> updateStatus(
            @PathVariable Integer benefitId,
            @Validated @RequestBody BenefitStatusRequest request,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        return ResponseEntity.ok(benefitAdminService.updateStatus(benefitId, request.status()));
    }

    @DeleteMapping("/{benefitId}")
    @Operation(summary = "刪除特典",
            description = "僅用於誤植或重複資料，一般請用下架。有會員持有紀錄時回409。"
                    + "會一併刪除該特典的庫存回報、貢獻與影城關聯")
    public ResponseEntity<Void> delete(
            @PathVariable Integer benefitId,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        benefitAdminService.delete(benefitId);
        return ResponseEntity.noContent().build();
    }
}
