package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.dto.MovieResponse;
import com.collection.benefitsjourney.service.CinemaCrawlerService;
import com.collection.benefitsjourney.service.MovieCrawlerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/crawler/cinemas")
@Tag(name = "爬蟲")
public class CrawlerController {
    private CinemaCrawlerService cinemaCrawlerService;
    private MovieCrawlerService movieCrawlerService;

    public CrawlerController(CinemaCrawlerService cinemaCrawlerService, MovieCrawlerService movieCrawlerService) {
        this.cinemaCrawlerService = cinemaCrawlerService;
        this.movieCrawlerService = movieCrawlerService;
    }

    @GetMapping("")
    @Operation(summary = "爬取並同步影城資料")
    public ResponseEntity<List<CinemaResponse>> syncCinemas() {
        return ResponseEntity.ok(cinemaCrawlerService.syncCinemas());
    }

    @GetMapping("/movies")
    @Operation(summary = "爬取並同步電影資料")
    public ResponseEntity<List<MovieResponse>> syncMovies() {
        return ResponseEntity.ok(
                movieCrawlerService.syncMovies());
    }
}
