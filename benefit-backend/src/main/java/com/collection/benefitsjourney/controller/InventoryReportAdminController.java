package com.collection.benefitsjourney.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.InventoryReportResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.service.InventoryReportAdminService;
import com.collection.benefitsjourney.service.UsersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory-reports")
@RequiredArgsConstructor
@Tag(name = "庫存回報管理（Admin）")
public class InventoryReportAdminController {

    private final InventoryReportAdminService inventoryReportAdminService;
    private final UsersService usersService;

    /** 非管理員回403 */
    private void requireAdmin(HttpServletRequest request) {
        Integer usersId = (Integer) request.getAttribute("usersId");
        UsersBean user = usersService.getUserById(usersId);

        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "您沒有權限查看此頁面");
        }
    }

    @GetMapping
    @Operation(summary = "查詢全部庫存回報", description = "依回報時間新到舊排序")
    public ResponseEntity<PageResponse<InventoryReportResponse>> findAllReports(
            @Parameter(description = "頁碼")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數")
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        return ResponseEntity.ok(
                inventoryReportAdminService.findAllReports(page, size));
    }

    @DeleteMapping("/{reportId}")
    @Operation(summary = "刪除指定庫存回報",
            description = "同步刪除貢獻紀錄，並重新計算該影城特典的庫存狀態")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Integer reportId,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        inventoryReportAdminService.deleteReport(reportId);
        return ResponseEntity.noContent().build();
    }
}