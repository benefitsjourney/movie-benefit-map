package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CinemaCrawlerResponse;
import com.collection.benefitsjourney.service.CinemaCrawlerService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/crawler/cinemas")
public class CrawlerController {
    private CinemaCrawlerService crawlerService;

    public CrawlerController(CinemaCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("")
    @Operation(summary = "影城爬蟲")
    public ResponseEntity<List<CinemaCrawlerResponse>> syncCinemas() {
        return ResponseEntity.ok(crawlerService.syncCinemas());
    }
}
