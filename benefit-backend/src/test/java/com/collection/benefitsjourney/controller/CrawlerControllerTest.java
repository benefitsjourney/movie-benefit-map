package com.collection.benefitsjourney.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.dto.MovieResponse;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.service.CrawlerCinemaService;
import com.collection.benefitsjourney.service.CrawlerMovieService;

@WebMvcTest(controllers = CrawlerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class CrawlerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlerCinemaService cinemaCrawlerService;

    @MockitoBean
    private CrawlerMovieService movieCrawlerService;

    private CinemaResponse cinema() {
        return new CinemaResponse(
                100, "t02a01", "信義威秀", County.TAIPEI,
                "台北市信義區松壽路20號", null, null,
                "https://example.com", true, true, null, null);
    }

    private MovieResponse movie() {
        return new MovieResponse(
                1, "fA001", "測試電影", "https://example.com/poster.jpg",
                true, LocalDateTime.of(2026, 10, 12, 0, 0), null, null);
    }

    @Nested
    @DisplayName("GET /crawler/cinemas")
    class SyncCinemasTests {

        @Test
        @DisplayName("同步成功應回傳 200 與影城資料")
        void success() throws Exception {
            when(cinemaCrawlerService.syncCinemas()).thenReturn(List.of(cinema()));

            mockMvc.perform(get("/crawler/cinemas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].cinemaId").value(100))
                    .andExpect(jsonPath("$[0].sourceId").value("t02a01"))
                    .andExpect(jsonPath("$[0].cinemaName").value("信義威秀"))
                    .andExpect(jsonPath("$[0].county").value("TAIPEI"))
                    .andExpect(jsonPath("$[0].isActive").value(true));

            verify(cinemaCrawlerService).syncCinemas();
            verifyNoInteractions(movieCrawlerService);
        }

        @Test
        @DisplayName("Service 回傳空集合應回傳 200 與空陣列")
        void emptyResult() throws Exception {
            when(cinemaCrawlerService.syncCinemas()).thenReturn(List.of());

            mockMvc.perform(get("/crawler/cinemas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("來源網站異常應回傳 502")
        void sourceError() throws Exception {
            when(cinemaCrawlerService.syncCinemas()).thenThrow(
                    new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "找不到影城縣市資料，來源網站結構可能已變更"));

            mockMvc.perform(get("/crawler/cinemas"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.message")
                            .value("找不到影城縣市資料，來源網站結構可能已變更"));
        }

        @Test
        @DisplayName("資料庫同步全部失敗應回傳 500")
        void databaseError() throws Exception {
            when(cinemaCrawlerService.syncCinemas()).thenThrow(
                    new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "影城資料抓取成功，但寫入資料庫失敗"));

            mockMvc.perform(get("/crawler/cinemas"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message")
                            .value("影城資料抓取成功，但寫入資料庫失敗"));
        }
    }

    @Nested
    @DisplayName("GET /crawler/movies")
    class SyncMoviesTests {

        @Test
        @DisplayName("同步成功應回傳 200 與電影資料")
        void success() throws Exception {
            when(movieCrawlerService.syncMovies()).thenReturn(List.of(movie()));

            mockMvc.perform(get("/crawler/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].movieId").value(1))
                    .andExpect(jsonPath("$[0].sourceId").value("fA001"))
                    .andExpect(jsonPath("$[0].movieName").value("測試電影"))
                    .andExpect(jsonPath("$[0].isVisible").value(true))
                    .andExpect(jsonPath("$[0].releaseDate").exists());

            verify(movieCrawlerService).syncMovies();
            verifyNoInteractions(cinemaCrawlerService);
        }

        @Test
        @DisplayName("Service 回傳空集合應回傳 200 與空陣列")
        void emptyResult() throws Exception {
            when(movieCrawlerService.syncMovies()).thenReturn(List.of());

            mockMvc.perform(get("/crawler/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("沒有成功同步任何電影應回傳 502")
        void noMoviesSynced() throws Exception {
            when(movieCrawlerService.syncMovies()).thenThrow(
                    new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "電影爬蟲未成功同步任何資料"));

            mockMvc.perform(get("/crawler/movies"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.message")
                            .value("電影爬蟲未成功同步任何資料"));
        }

        @Test
        @DisplayName("同步遭到中斷應回傳 503")
        void interrupted() throws Exception {
            when(movieCrawlerService.syncMovies()).thenThrow(
                    new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "電影同步遭到中斷"));

            mockMvc.perform(get("/crawler/movies"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503));
        }
    }

    @Nested
    @DisplayName("不支援的 HTTP Method 與未預期錯誤")
    class ErrorTests {

        @Test
        @DisplayName("POST /crawler/cinemas 應回傳 405")
        void postCinemas() throws Exception {
            mockMvc.perform(post("/crawler/cinemas"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405));

            verifyNoInteractions(cinemaCrawlerService, movieCrawlerService);
        }

        @Test
        @DisplayName("POST /crawler/movies 應回傳 405")
        void postMovies() throws Exception {
            mockMvc.perform(post("/crawler/movies"))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(cinemaCrawlerService, movieCrawlerService);
        }

        @Test
        @DisplayName("未預期的 Service 例外應回傳 500")
        void unexpectedError() throws Exception {
            when(movieCrawlerService.syncMovies())
                    .thenThrow(new IllegalStateException("unexpected"));

            mockMvc.perform(get("/crawler/movies"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("系統發生錯誤"));
        }
    }
}
