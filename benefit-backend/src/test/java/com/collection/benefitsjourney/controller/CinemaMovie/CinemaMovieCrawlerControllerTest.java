package com.collection.benefitsjourney.controller.CinemaMovie;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CinemaMovie.CinemaResponse;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerSyncResult;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.controller.AdminRequestConfig;
import com.collection.benefitsjourney.service.CinemaMovie.CrawlerCinemaService;
import com.collection.benefitsjourney.service.CinemaMovie.CrawlerMovieService;
import com.collection.benefitsjourney.service.UsersService;

@WebMvcTest(controllers = CinemaMovieCrawlerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminRequestConfig.class)
class CinemaMovieCrawlerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlerCinemaService crawlerCinemaService;

    @MockitoBean
    private CrawlerMovieService crawlerMovieService;

    @MockitoBean
    private UsersService usersService;

    @BeforeEach
    void setUpAdmin() {
        AdminRequestConfig.stubAdmin(usersService);
    }

    private CinemaResponse cinema() {
        return new CinemaResponse(
                100, "t02a01", "https://www.atmovies.com.tw/showtime/t02a01/", "信義威秀", County.TAIPEI,
                "台北市信義區松壽路20號", null, null,
                "https://example.com", true, true, null, null);
    }

    private MovieResponse movie() {
        return new MovieResponse(
                1, "fA001", "https://www.atmovies.com.tw/movie/fA001/", "測試電影",
                "https://example.com/poster.jpg",
                true, LocalDateTime.of(2026, 10, 12, 0, 0), null, null);
    }

    @Nested
    @DisplayName("GET /cinema-movie/crawler/cinemas")
    class SyncCinemasTests {

        @Test
        @DisplayName("同步成功應回傳 200 與影城資料")
        void success() throws Exception {
            when(crawlerCinemaService.syncCinemas()).thenReturn(
                    CinemaMovieCrawlerSyncResult.of(3, 1, 2, 4, 5, 8, List.of(cinema())));

            mockMvc.perform(get("/cinema-movie/crawler/cinemas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fetchedCount").value(3))
                    .andExpect(jsonPath("$.createdCount").value(1))
                    .andExpect(jsonPath("$.skippedCount").value(2))
                    .andExpect(jsonPath("$.updatedCount").value(4))
                    .andExpect(jsonPath("$.reviewCount").value(5))
                    .andExpect(jsonPath("$.elapsedSeconds").value(8))
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.items[0].cinemaId").value(100))
                    .andExpect(jsonPath("$.items[0].sourceId").value("t02a01"))
                    .andExpect(jsonPath("$.items[0].sourceUrl")
                            .value("https://www.atmovies.com.tw/showtime/t02a01/"))
                    .andExpect(jsonPath("$.items[0].cinemaName").value("信義威秀"))
                    .andExpect(jsonPath("$.items[0].county").value("TAIPEI"))
                    .andExpect(jsonPath("$.items[0].isActive").value(true));

            verify(crawlerCinemaService).syncCinemas();
            verifyNoInteractions(crawlerMovieService);
        }

        @Test
        @DisplayName("Service 回傳空結果應回傳 200 與統計")
        void emptyResult() throws Exception {
            when(crawlerCinemaService.syncCinemas()).thenReturn(
                    CinemaMovieCrawlerSyncResult.of(0, 0, 0, 0, 0, 0, List.of()));

            mockMvc.perform(get("/cinema-movie/crawler/cinemas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fetchedCount").value(0))
                    .andExpect(jsonPath("$.createdCount").value(0))
                    .andExpect(jsonPath("$.skippedCount").value(0))
                    .andExpect(jsonPath("$.updatedCount").value(0))
                    .andExpect(jsonPath("$.reviewCount").value(0))
                    .andExpect(jsonPath("$.elapsedSeconds").value(0))
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isEmpty());
        }

        @Test
        @DisplayName("來源網站異常應回傳 502")
        void sourceError() throws Exception {
            when(crawlerCinemaService.syncCinemas()).thenThrow(
                    new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "找不到影城縣市資料，來源網站結構可能已變更"));

            mockMvc.perform(get("/cinema-movie/crawler/cinemas"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.message")
                            .value("找不到影城縣市資料，來源網站結構可能已變更"));
        }

        @Test
        @DisplayName("資料庫同步全部失敗應回傳 500")
        void databaseError() throws Exception {
            when(crawlerCinemaService.syncCinemas()).thenThrow(
                    new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "影城資料抓取成功，但寫入資料庫失敗"));

            mockMvc.perform(get("/cinema-movie/crawler/cinemas"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message")
                            .value("影城資料抓取成功，但寫入資料庫失敗"));
        }
    }

    @Nested
    @DisplayName("GET /cinema-movie/crawler/movies")
    class SyncMoviesTests {

        @Test
        @DisplayName("同步成功應回傳 200 與電影資料")
        void success() throws Exception {
            when(crawlerMovieService.syncMovies()).thenReturn(
                    CinemaMovieCrawlerSyncResult.of(4, 2, 3, 1, 1, 6, List.of(movie())));

            mockMvc.perform(get("/cinema-movie/crawler/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fetchedCount").value(4))
                    .andExpect(jsonPath("$.createdCount").value(2))
                    .andExpect(jsonPath("$.skippedCount").value(3))
                    .andExpect(jsonPath("$.updatedCount").value(1))
                    .andExpect(jsonPath("$.reviewCount").value(1))
                    .andExpect(jsonPath("$.elapsedSeconds").value(6))
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.items[0].movieId").value(1))
                    .andExpect(jsonPath("$.items[0].sourceId").value("fA001"))
                    .andExpect(jsonPath("$.items[0].sourceUrl")
                            .value("https://www.atmovies.com.tw/movie/fA001/"))
                    .andExpect(jsonPath("$.items[0].movieName").value("測試電影"))
                    .andExpect(jsonPath("$.items[0].isVisible").value(true))
                    .andExpect(jsonPath("$.items[0].releaseDate").exists());

            verify(crawlerMovieService).syncMovies();
            verifyNoInteractions(crawlerCinemaService);
        }

        @Test
        @DisplayName("Service 回傳空結果應回傳 200 與統計")
        void emptyResult() throws Exception {
            when(crawlerMovieService.syncMovies()).thenReturn(
                    CinemaMovieCrawlerSyncResult.of(0, 0, 0, 0, 0, 0, List.of()));

            mockMvc.perform(get("/cinema-movie/crawler/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.createdCount").value(0))
                    .andExpect(jsonPath("$.skippedCount").value(0));
        }

        @Test
        @DisplayName("沒有成功同步任何電影應回傳 502")
        void noMoviesSynced() throws Exception {
            when(crawlerMovieService.syncMovies()).thenThrow(
                    new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "電影爬蟲未成功同步任何資料"));

            mockMvc.perform(get("/cinema-movie/crawler/movies"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.message")
                            .value("電影爬蟲未成功同步任何資料"));
        }

        @Test
        @DisplayName("同步遭到中斷應回傳 503")
        void interrupted() throws Exception {
            when(crawlerMovieService.syncMovies()).thenThrow(
                    new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "電影同步遭到中斷"));

            mockMvc.perform(get("/cinema-movie/crawler/movies"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503));
        }
    }

    @Nested
    @DisplayName("不支援的 HTTP Method 與未預期錯誤")
    class ErrorTests {

        @Test
        @DisplayName("POST /cinema-movie/crawler/cinemas 應回傳 405")
        void postCinemas() throws Exception {
            mockMvc.perform(post("/cinema-movie/crawler/cinemas"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405));

            verifyNoInteractions(crawlerCinemaService, crawlerMovieService);
        }

        @Test
        @DisplayName("POST /cinema-movie/crawler/movies 應回傳 405")
        void postMovies() throws Exception {
            mockMvc.perform(post("/cinema-movie/crawler/movies"))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(crawlerCinemaService, crawlerMovieService);
        }

        @Test
        @DisplayName("未預期的 Service 例外應回傳 500")
        void unexpectedError() throws Exception {
            when(crawlerMovieService.syncMovies())
                    .thenThrow(new IllegalStateException("unexpected"));

            mockMvc.perform(get("/cinema-movie/crawler/movies"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("系統發生錯誤"));
        }
    }
}
