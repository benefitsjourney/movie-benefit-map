package com.collection.benefitsjourney.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.collection.benefitsjourney.dto.CrawlerLogRequest;
import com.collection.benefitsjourney.dto.CrawlerLogResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CrawlerLogSort;
import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.service.CrawlerLogService;

@WebMvcTest(controllers = CrawlerLogController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class CrawlerLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlerLogService crawlerLogService;

    @Nested
    @DisplayName("GET /crawler-logs")
    class FindAllTests {

        @Test
        @DisplayName("未帶查詢參數時，應使用 Controller 預設值")
        void defaultParameters() throws Exception {
            when(crawlerLogService.findAll(
                    isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CrawlerLogSort.SortBy.EXECUTED_AT),
                    eq(SortOrder.DESC)))
                    .thenReturn(new PageResponse<>(List.of(logResponse()), 1, 20, 1, 1));

            mockMvc.perform(get("/crawler-logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].crawlerLogId").value(3))
                    .andExpect(jsonPath("$.content[0].crawlerType").value("MOVIE"))
                    .andExpect(jsonPath("$.content[0].crawlerStatus").value("SUCCESS"));

            verify(crawlerLogService).findAll(
                    null, null,
                    1, 20,
                    CrawlerLogSort.SortBy.EXECUTED_AT,
                    SortOrder.DESC);
        }

        @Test
        @DisplayName("所有查詢參數應正確傳入 Service")
        void allParameters() throws Exception {
            mockMvc.perform(get("/crawler-logs")
                    .param("crawlerType", "BENEFIT")
                    .param("crawlerStatus", "PARTIAL")
                    .param("orderBy", "FINISHED_AT")
                    .param("sort", "ASC")
                    .param("page", "2")
                    .param("size", "10"))
                    .andExpect(status().isOk());

            verify(crawlerLogService).findAll(
                    CrawlerType.BENEFIT,
                    CrawlerStatus.PARTIAL,
                    2,
                    10,
                    CrawlerLogSort.SortBy.FINISHED_AT,
                    SortOrder.ASC);
        }

        @Test
        @DisplayName("無效的爬蟲類型應回傳 400")
        void invalidCrawlerType() throws Exception {
            mockMvc.perform(get("/crawler-logs").param("crawlerType", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("crawlerType 參數值無效：ABC"));

            verifyNoInteractions(crawlerLogService);
        }

        @Test
        @DisplayName("無效的執行狀態應回傳 400")
        void invalidCrawlerStatus() throws Exception {
            mockMvc.perform(get("/crawler-logs").param("crawlerStatus", "DONE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("crawlerStatus 參數值無效：DONE"));

            verifyNoInteractions(crawlerLogService);
        }
    }

    @Nested
    @DisplayName("POST /crawler-logs")
    class CreateCrawlerLogTests {

        @Test
        @DisplayName("完整 JSON 應正確轉換為 CrawlerLogRequest")
        void validRequest() throws Exception {
            String json = """
                    {
                      "crawlerType": "MOVIE",
                      "crawlerStatus": "SUCCESS",
                      "fetchedCount": 10,
                      "createdCount": 2,
                      "updatedCount": 3,
                      "skippedCount": 1,
                      "pendingReviewCount": 4,
                      "message": "電影同步完成",
                      "executedAt": "2026-10-20T10:00:00",
                      "finishedAt": "2026-10-20T10:01:30"
                    }
                    """;

            mockMvc.perform(post("/crawler-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CrawlerLogRequest> captor = ArgumentCaptor.forClass(CrawlerLogRequest.class);
            verify(crawlerLogService).createCrawlerLog(captor.capture());
            CrawlerLogRequest request = captor.getValue();

            assertEquals(CrawlerType.MOVIE, request.crawlerType());
            assertEquals(CrawlerStatus.SUCCESS, request.crawlerStatus());
            assertEquals(10, request.fetchedCount());
            assertEquals(2, request.createdCount());
            assertEquals(3, request.updatedCount());
            assertEquals(1, request.skippedCount());
            assertEquals(4, request.pendingReviewCount());
            assertEquals("電影同步完成", request.message());
            assertEquals(LocalDateTime.of(2026, 10, 20, 10, 0), request.executedAt());
            assertEquals(LocalDateTime.of(2026, 10, 20, 10, 1, 30), request.finishedAt());
        }

        @Test
        @DisplayName("省略訊息時應維持 null")
        void messageOptional() throws Exception {
            String json = """
                    {
                      "crawlerType": "CINEMA",
                      "crawlerStatus": "FAILURE",
                      "fetchedCount": 0,
                      "createdCount": 0,
                      "updatedCount": 0,
                      "skippedCount": 0,
                      "pendingReviewCount": 0,
                      "executedAt": "2026-10-20T10:00:00",
                      "finishedAt": "2026-10-20T10:00:05"
                    }
                    """;

            mockMvc.perform(post("/crawler-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CrawlerLogRequest> captor = ArgumentCaptor.forClass(CrawlerLogRequest.class);
            verify(crawlerLogService).createCrawlerLog(captor.capture());
            assertNull(captor.getValue().message());
        }

        @Test
        @DisplayName("缺少必填欄位應回傳 400")
        void missingRequired() throws Exception {
            mockMvc.perform(post("/crawler-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(crawlerLogService, never()).createCrawlerLog(any());
        }

        @Test
        @DisplayName("筆數為負數應回傳 400")
        void negativeCount() throws Exception {
            String json = """
                    {
                      "crawlerType": "BENEFIT",
                      "crawlerStatus": "PARTIAL",
                      "fetchedCount": -1,
                      "createdCount": 0,
                      "updatedCount": 0,
                      "skippedCount": 0,
                      "pendingReviewCount": 0,
                      "executedAt": "2026-10-20T10:00:00",
                      "finishedAt": "2026-10-20T10:00:05"
                    }
                    """;

            mockMvc.perform(post("/crawler-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("抓取筆數不可小於0"));

            verify(crawlerLogService, never()).createCrawlerLog(any());
        }
    }

    private static CrawlerLogResponse logResponse() {
        return new CrawlerLogResponse(
                3,
                CrawlerType.MOVIE,
                CrawlerStatus.SUCCESS,
                10,
                2,
                3,
                1,
                4,
                "電影同步完成",
                LocalDateTime.of(2026, 10, 20, 10, 0),
                LocalDateTime.of(2026, 10, 20, 10, 1, 30));
    }
}
