package com.collection.benefitsjourney.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.service.BenefitExcelService;
import com.collection.benefitsjourney.service.UsersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/export/benefits")
@Tag(name = "資料匯出")
public class BenefitExportController {

    private final BenefitExcelService benefitExcelService;
    private final UsersService usersService;

    public BenefitExportController(BenefitExcelService benefitExcelService, UsersService usersService) {
        this.benefitExcelService = benefitExcelService;
        this.usersService = usersService;
    }

    /** 非管理員回403（做法同BenefitCrawlerController） */
    private void requireAdmin(HttpServletRequest request) {
        Integer usersId = (Integer) request.getAttribute("usersId");
        UsersBean user = usersService.getUserById(usersId);
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您沒有權限使用此功能");
        }
    }

    @GetMapping(produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "匯出特典Excel（Admin）",
            description = "一個檔案三張工作表：特典、特典_影城（中介表＋庫存）、回報單。"
                    + "沒有審核前端時，可以下載後對照「回報單」工作表，再用Swagger或Postman逐筆核准或駁回")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "回報單工作表只放待審核（PENDING）。false＝放全部狀態")
            @RequestParam(defaultValue = "true") boolean onlyPending,
            HttpServletRequest request) {

        requireAdmin(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"benefits.xlsx\"")
                .body(benefitExcelService.export(onlyPending));
    }
}