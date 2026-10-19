package com.collection.benefitsjourney.controller.CinemaMovie;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CinemaMovie.CinemaResponse;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerSyncResult;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;
import com.collection.benefitsjourney.service.CinemaMovie.CrawlerCinemaService;
import com.collection.benefitsjourney.service.CinemaMovie.CrawlerMovieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/cinema-movie/crawler")
@Tag(name = "影城電影爬蟲")
public class CinemaMovieCrawlerController {
    private CrawlerCinemaService cinemaCrawlerService;
    private CrawlerMovieService movieCrawlerService;

    public CinemaMovieCrawlerController(CrawlerCinemaService cinemaCrawlerService,
            CrawlerMovieService movieCrawlerService) {
        this.cinemaCrawlerService = cinemaCrawlerService;
        this.movieCrawlerService = movieCrawlerService;
    }

    @GetMapping("/cinemas")
    @Operation(summary = "爬取並同步影城資料")
    public ResponseEntity<CinemaMovieCrawlerSyncResult<CinemaResponse>> syncCinemas() {
        return ResponseEntity.ok(cinemaCrawlerService.syncCinemas());
    }

    @GetMapping("/movies")
    @Operation(summary = "爬取並同步電影資料")
    public ResponseEntity<CinemaMovieCrawlerSyncResult<MovieResponse>> syncMovies() {
        return ResponseEntity.ok(
                movieCrawlerService.syncMovies());
    }
}
