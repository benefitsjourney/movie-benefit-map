package com.collection.benefitsjourney.controller.CinemaMovie;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.CinemaMovie.CinemaResponse;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerSyncResult;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.service.CinemaMovie.CrawlerCinemaService;
import com.collection.benefitsjourney.service.CinemaMovie.CrawlerMovieService;
import com.collection.benefitsjourney.service.UsersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/cinema-movie/crawler")
@Tag(name = "影城電影爬蟲")
public class CinemaMovieCrawlerController {
    private CrawlerCinemaService crawlerCinemaService;
    private CrawlerMovieService crawlerMovieService;
    private final UsersService usersService;

    public CinemaMovieCrawlerController(CrawlerCinemaService crawlerCinemaService,
            CrawlerMovieService crawlerMovieService,
            UsersService usersService) {
        this.crawlerCinemaService = crawlerCinemaService;
        this.crawlerMovieService = crawlerMovieService;
        this.usersService = usersService;
    }

    @GetMapping("/cinemas")
    @Operation(summary = "爬取並同步影城資料")
    public ResponseEntity<CinemaMovieCrawlerSyncResult<CinemaResponse>> syncCinemas(
            HttpServletRequest httpRequest) {
        ExceptionUtils.requireAdmin(httpRequest, usersService);
        return ResponseEntity.ok(crawlerCinemaService.syncCinemas());
    }

    @GetMapping("/movies")
    @Operation(summary = "爬取並同步電影資料")
    public ResponseEntity<CinemaMovieCrawlerSyncResult<MovieResponse>> syncMovies(
            HttpServletRequest httpRequest) {
        ExceptionUtils.requireAdmin(httpRequest, usersService);
        return ResponseEntity.ok(
                crawlerMovieService.syncMovies());
    }
}
