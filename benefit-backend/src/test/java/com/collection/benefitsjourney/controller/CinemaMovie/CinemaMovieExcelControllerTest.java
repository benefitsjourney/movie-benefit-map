package com.collection.benefitsjourney.controller.CinemaMovie;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.service.CinemaMovie.CinemaMovieExcelService;

/**
 * 只測試 CinemaMovieExcelController 的 HTTP 路由、參數轉換、回應 Header 和例外處理。
 * CinemaMovieExcelService 使用 Mock，不產生實際 Excel，也不連線資料庫。
 */
@WebMvcTest(controllers = CinemaMovieExcelController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class CinemaMovieExcelControllerTest {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CinemaMovieExcelService excelService;

    @Nested
    @DisplayName("GET /cinema-movie/export - 正常匯出")
    class ExportTests {

        @Test
        @DisplayName("CINEMA 應回傳 Excel、正確的檔名和二進位內容")
        void exportCinema() throws Exception {
            byte[] bytes = { 1, 2, 3, 4 };
            when(excelService.export(CinemaResourceType.CINEMA)).thenReturn(bytes);

            mockMvc.perform(get("/cinema-movie/export").param("type", "CINEMA"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(EXCEL_CONTENT_TYPE))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"cinema.xlsx\""))
                    .andExpect(content().bytes(bytes));

            verify(excelService).export(CinemaResourceType.CINEMA);
        }

        @Test
        @DisplayName("MOVIE 應回傳 Excel、正確的檔名和二進位內容")
        void exportMovie() throws Exception {
            byte[] bytes = { 5, 6, 7 };
            when(excelService.export(CinemaResourceType.MOVIE)).thenReturn(bytes);

            mockMvc.perform(get("/cinema-movie/export").param("type", "MOVIE"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(EXCEL_CONTENT_TYPE))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"movie.xlsx\""))
                    .andExpect(content().bytes(bytes));

            verify(excelService).export(CinemaResourceType.MOVIE);
        }

        @Test
        @DisplayName("CRAWLER_LOG 應回傳 Excel、正確的檔名和二進位內容")
        void exportCrawlerLog() throws Exception {
            byte[] bytes = { 8, 9 };
            when(excelService.export(CinemaResourceType.CRAWLER_LOG)).thenReturn(bytes);

            mockMvc.perform(get("/cinema-movie/export").param("type", "CRAWLER_LOG"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(EXCEL_CONTENT_TYPE))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"crawler_log.xlsx\""))
                    .andExpect(content().bytes(bytes));

            verify(excelService).export(CinemaResourceType.CRAWLER_LOG);
        }
    }

    @Nested
    @DisplayName("GET /cinema-movie/export - 參數驗證")
    class ParameterTests {

        @Test
        @DisplayName("缺少 type 應回傳 400，不呼叫 Service")
        void missingType() throws Exception {
            mockMvc.perform(get("/cinema-movie/export"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(excelService);
        }

        @Test
        @DisplayName("無效的 MoviegoingCategory 應回傳 400，不呼叫 Service")
        void invalidType() throws Exception {
            mockMvc.perform(get("/cinema-movie/export").param("type", "UNKNOWN"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(excelService);
        }
    }

    @Nested
    @DisplayName("不支援的 HTTP Method")
    class HttpMethodTests {

        @Test
        @DisplayName("POST /cinema-movie/export 應回傳 405")
        void postExport() throws Exception {
            mockMvc.perform(post("/cinema-movie/export").param("type", "CINEMA"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405));

            verifyNoInteractions(excelService);
        }

        @Test
        @DisplayName("PUT /cinema-movie/export 應回傳 405")
        void putExport() throws Exception {
            mockMvc.perform(put("/cinema-movie/export").param("type", "CINEMA"))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(excelService);
        }

        @Test
        @DisplayName("DELETE /cinema-movie/export 應回傳 405")
        void deleteExport() throws Exception {
            mockMvc.perform(delete("/cinema-movie/export").param("type", "CINEMA"))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(excelService);
        }
    }

    @Nested
    @DisplayName("未預期的程式錯誤")
    class UnexpectedErrorTests {

        @Test
        @DisplayName("Service 匯出失敗時應回傳 500")
        void exportFailure() throws Exception {
            when(excelService.export(CinemaResourceType.CINEMA))
                    .thenThrow(new IllegalStateException("Excel 匯出失敗"));

            mockMvc.perform(get("/cinema-movie/export").param("type", "CINEMA"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("系統發生錯誤"));

            verify(excelService).export(CinemaResourceType.CINEMA);
        }

        @Test
        @DisplayName("Accept 為 Excel 時，業務錯誤仍回傳 JSON")
        void exportNotFoundReturnsJson() throws Exception {
            when(excelService.export(CinemaResourceType.CINEMA))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "找不到影城"));

            mockMvc.perform(get("/cinema-movie/export")
                    .param("type", "CINEMA")
                    .accept(EXCEL_CONTENT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("找不到影城"));

            verify(excelService).export(CinemaResourceType.CINEMA);
        }
    }
}
