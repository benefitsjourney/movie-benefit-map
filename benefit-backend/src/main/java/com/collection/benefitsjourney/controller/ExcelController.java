
package com.collection.benefitsjourney.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.collection.benefitsjourney.enums.ExportType;
import com.collection.benefitsjourney.service.ExcelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/export")
@Tag(name = "資料匯出")
public class ExcelController {

    private final ExcelService excelService;

    @GetMapping(produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "匯出Excel資料")
    public ResponseEntity<byte[]> export(@RequestParam ExportType type) {

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + type.name().toLowerCase() + ".xlsx\"")
                .body(excelService.export(type));
    }
}
