package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CrawlerLogRequest;
import com.collection.benefitsjourney.dto.CrawlerLogResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CrawlerLogSort;
import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.service.CrawlerLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/crawler-logs")
@Tag(name = "爬蟲紀錄")
public class CrawlerLogController {
    private final CrawlerLogService crawlerLogService;

    public CrawlerLogController(CrawlerLogService crawlerLogService) {
        this.crawlerLogService = crawlerLogService;
    }

    @GetMapping
    @Operation(summary = "取得爬蟲紀錄")
    public ResponseEntity<PageResponse<CrawlerLogResponse>> findAll(
            @Parameter(description = "爬蟲類型") @RequestParam(required = false) CrawlerType crawlerType,

            @Parameter(description = "執行狀態") @RequestParam(required = false) CrawlerStatus crawlerStatus,

            @Parameter(description = "排序欄位") @RequestParam(defaultValue = "EXECUTED_AT") CrawlerLogSort.SortBy orderBy,

            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") SortOrder sort,

            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                crawlerLogService.findAll(
                        crawlerType,
                        crawlerStatus,
                        page,
                        size,
                        orderBy,
                        sort));
    }

    @PostMapping("")
    @Operation(summary = "新增爬蟲紀錄")
    public ResponseEntity<CrawlerLogResponse> createCrawlerLog(
            @Validated({ Default.class, CrawlerLogRequest.OnCreate.class }) @RequestBody CrawlerLogRequest request) {

        return ResponseEntity.ok(crawlerLogService.createCrawlerLog(request));
    }
}
