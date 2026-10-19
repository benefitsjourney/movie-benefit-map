package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.UseBenefitCreateRequest;
import com.collection.benefitsjourney.dto.UseBenefitResponse;
import com.collection.benefitsjourney.service.UseBenefitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/use-benefits")
@RequiredArgsConstructor
@Tag(name = "收藏庫（會員）", description = "持有紀錄的新增、查詢、刪除")
public class UseBenefitController {

    private final UseBenefitService useBenefitService;

    @PostMapping
    @Operation(summary = "新增持有紀錄",
            description = "body帶benefitId。已上架、已下架的特典都可以新增，同一個特典只能收藏一次（重複回409）")
    public ResponseEntity<UseBenefitResponse> create(
            @Validated @RequestBody UseBenefitCreateRequest request,
            HttpServletRequest httpRequest) {

        Integer usersId = getUsersId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useBenefitService.create(usersId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "查看自己的持有清單", description = "新到舊，不分特典上下架，含照片")
    public ResponseEntity<List<UseBenefitResponse>> findMine(HttpServletRequest httpRequest) {
        Integer usersId = getUsersId(httpRequest);
        return ResponseEntity.ok(useBenefitService.findMine(usersId));
    }

    @DeleteMapping("/{useBenefitId}")
    @Operation(summary = "刪除持有紀錄",
            description = "只能刪自己的（否則403）。已建立交換刊登的不能刪（409）")
    public ResponseEntity<Void> delete(
            @PathVariable Integer useBenefitId,
            HttpServletRequest httpRequest) {

        Integer usersId = getUsersId(httpRequest);
        useBenefitService.delete(usersId, useBenefitId);
        return ResponseEntity.noContent().build();
    }

    /** 從JWT Filter放進request的usersId取得登入者（寫法同InventoryReportController） */
    private Integer getUsersId(HttpServletRequest request) {
        Object value = request.getAttribute("usersId");
        if (!(value instanceof Integer)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "尚未取得登入會員資料");
        }
        return (Integer) value;
    }
}