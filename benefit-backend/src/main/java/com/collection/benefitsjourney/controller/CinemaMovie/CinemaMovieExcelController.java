
package com.collection.benefitsjourney.controller.CinemaMovie;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.service.CinemaMovie.CinemaMovieExcelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cinema-movie/export")
@Tag(name = "影城電影Excel匯出")
public class CinemaMovieExcelController {

    private final CinemaMovieExcelService excelService;

    @GetMapping(produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "匯出Excel資料")
    public ResponseEntity<byte[]> export(@RequestParam CinemaResourceType type) {

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + type.name().toLowerCase() + ".xlsx\"")
                .body(excelService.export(type));
    }
}
