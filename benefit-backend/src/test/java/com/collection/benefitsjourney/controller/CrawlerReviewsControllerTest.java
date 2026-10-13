package com.collection.benefitsjourney.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CrawlerReviewRequest;
import com.collection.benefitsjourney.dto.CrawlerReviewResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.CrawlerReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.jwt.JsonWebTokenUtility;
import com.collection.benefitsjourney.service.CrawlerReviewsService;

@WebMvcTest(controllers = CrawlerReviewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class CrawlerReviewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlerReviewsService crawlerReviewsService;

    @Nested
    @DisplayName("GET /crawler-reviews")
    class FindAllTests {

        @Test
        @DisplayName("未帶查詢參數時，應使用 Controller 預設值")
        void defaultParameters() throws Exception {
            when(crawlerReviewsService.findAll(
                    isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID),
                    eq(SortOrder.ASC)))
                    .thenReturn(new PageResponse<>(List.of(reviewResponse()), 1, 20, 1, 1));

            mockMvc.perform(get("/crawler-reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].crawlerReviewId").value(7))
                    .andExpect(jsonPath("$.content[0].crawlerType").value("MOVIE"))
                    .andExpect(jsonPath("$.content[0].fieldName").value("movieName"))
                    .andExpect(jsonPath("$.content[0].reviewStatus").value("PENDING"));

            verify(crawlerReviewsService).findAll(
                    null, null,
                    1, 20,
                    CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID,
                    SortOrder.ASC);
        }

        @Test
        @DisplayName("所有查詢參數應正確傳入 Service")
        void allParameters() throws Exception {
            mockMvc.perform(get("/crawler-reviews")
                    .param("crawlerType", "CINEMA")
                    .param("reviewStatus", "LOCKED")
                    .param("orderBy", "CRAWLED_AT")
                    .param("sort", "DESC")
                    .param("page", "3")
                    .param("size", "20"))
                    .andExpect(status().isOk());

            verify(crawlerReviewsService).findAll(
                    CinemaResourceType.CINEMA,
                    ReviewStatus.LOCKED,
                    3,
                    20,
                    CrawlerReviewSort.SortBy.CRAWLED_AT,
                    SortOrder.DESC);
        }

        @Test
        @DisplayName("無效的爬蟲類型應回傳 400")
        void invalidCrawlerType() throws Exception {
            mockMvc.perform(get("/crawler-reviews").param("crawlerType", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("crawlerType 參數值無效：ABC"));

            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("無效的審查狀態應回傳 400")
        void invalidReviewStatus() throws Exception {
            mockMvc.perform(get("/crawler-reviews").param("reviewStatus", "DONE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("reviewStatus 參數值無效：DONE"));

            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("無效的排序欄位應回傳 400")
        void invalidOrderBy() throws Exception {
            mockMvc.perform(get("/crawler-reviews").param("orderBy", "FIELD_NAME"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("orderBy 參數值無效：FIELD_NAME"));

            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("無效的排序方向應回傳 400")
        void invalidSort() throws Exception {
            mockMvc.perform(get("/crawler-reviews").param("sort", "UP"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("sort 參數值無效：UP"));

            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("page 不是整數應回傳 400")
        void invalidPage() throws Exception {
            mockMvc.perform(get("/crawler-reviews").param("page", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("size 不是整數應回傳 400")
        void invalidSize() throws Exception {
            mockMvc.perform(get("/crawler-reviews").param("size", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("Service 回傳 BAD_REQUEST 應對應 HTTP 400")
        void serviceBadRequest() throws Exception {
            when(crawlerReviewsService.findAll(
                    isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID),
                    eq(SortOrder.ASC)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "查詢參數錯誤"));

            mockMvc.perform(get("/crawler-reviews"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("查詢參數錯誤"));
        }
    }

    @Nested
    @DisplayName("POST /crawler-reviews")
    class CreateCrawlerReviewTests {

        @Test
        @DisplayName("完整 JSON 應正確轉換為 CrawlerReviewRequest")
        void validRequest() throws Exception {
            String json = """
                    {
                      "crawlerType": "MOVIE",
                      "entityId": 12,
                      "fieldName": "movieName",
                      "crawlerValue": "新片名",
                      "previousValue": "舊片名",
                      "reviewStatus": "RESUMED",
                      "crawledAt": "2026-10-08T10:33:00",
                      "reviewedAt": "2026-10-09T11:00:00",
                      "unlockedAt": "2026-10-10T12:15:30"
                    }
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CrawlerReviewRequest> captor = ArgumentCaptor.forClass(CrawlerReviewRequest.class);
            verify(crawlerReviewsService).createCrawlerReview(captor.capture());
            CrawlerReviewRequest request = captor.getValue();

            assertEquals(CinemaResourceType.MOVIE, request.crawlerType());
            assertEquals(12, request.entityId());
            assertEquals("movieName", request.fieldName());
            assertEquals("新片名", request.crawlerValue());
            assertEquals("舊片名", request.previousValue());
            assertEquals(ReviewStatus.RESUMED, request.reviewStatus());
            assertEquals(LocalDateTime.of(2026, 10, 8, 10, 33), request.crawledAt());
            assertEquals(LocalDateTime.of(2026, 10, 9, 11, 0), request.reviewedAt());
            assertEquals(LocalDateTime.of(2026, 10, 10, 12, 15, 30), request.unlockedAt());
        }

        @Test
        @DisplayName("省略的選填欄位應維持 null，由 Service 設定預設值")
        void minimalRequest() throws Exception {
            String json = """
                    {
                      "crawlerType": "CINEMA",
                      "entityId": 3,
                      "fieldName": "cinemaAddress"
                    }
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CrawlerReviewRequest> captor = ArgumentCaptor.forClass(CrawlerReviewRequest.class);
            verify(crawlerReviewsService).createCrawlerReview(captor.capture());
            CrawlerReviewRequest request = captor.getValue();

            assertEquals(CinemaResourceType.CINEMA, request.crawlerType());
            assertEquals(3, request.entityId());
            assertEquals("cinemaAddress", request.fieldName());
            assertNull(request.crawlerValue());
            assertNull(request.previousValue());
            assertNull(request.reviewStatus());
            assertNull(request.crawledAt());
            assertNull(request.reviewedAt());
            assertNull(request.unlockedAt());
        }

        @Test
        @DisplayName("未提供 RequestBody 應回傳 400")
        void noBody() throws Exception {
            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("不完整 JSON 應回傳 400")
        void invalidJson() throws Exception {
            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"crawlerType\":\"MOVIE\","))
                    .andExpect(status().isBadRequest());

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("爬蟲類型無效應回傳 400")
        void invalidCrawlerType() throws Exception {
            String json = """
                    {"crawlerType":"ABC","entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("crawlerType 參數值無效：ABC"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("審查狀態無效應回傳 400")
        void invalidReviewStatus() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName","reviewStatus":"DONE"}
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("reviewStatus 參數值無效：DONE"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("日期時間格式無效應回傳 400")
        void invalidDateTime() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName","crawledAt":"ABC"}
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("日期時間格式錯誤，應為 yyyy-MM-dd HH:mm:ss"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("未提供爬蟲類型應回傳 400")
        void missingCrawlerType() throws Exception {
            String json = """
                    {"entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("爬蟲類型不能為空"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("未提供對應資料 ID 應回傳 400")
        void missingEntityId() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("對應資料ID不能為空"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("欄位名稱為空白應回傳 400")
        void blankFieldName() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":" "}
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("欄位名稱不能為空"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("欄位名稱超過 64 字元應回傳 400")
        void fieldNameTooLong() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"%s"}
                    """.formatted("a".repeat(65));

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("欄位名稱長度不可超過64字元"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("爬蟲值超過 512 字元應回傳 400")
        void crawlerValueTooLong() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName","crawlerValue":"%s"}
                    """.formatted("a".repeat(513));

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("爬蟲值長度不可超過512字元"));

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("非 application/json 應回傳 415")
        void noContentType() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/crawler-reviews").content(json))
                    .andExpect(status().isUnsupportedMediaType());

            verify(crawlerReviewsService, never()).createCrawlerReview(any());
        }

        @Test
        @DisplayName("已存在未鎖定的相同審核時應回傳 400")
        void duplicateReview() throws Exception {
            when(crawlerReviewsService.createCrawlerReview(any(CrawlerReviewRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "已存在相同資料的爬蟲審核，且審查狀態不是鎖定同步，無法新增"));

            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("已存在相同資料的爬蟲審核，且審查狀態不是鎖定同步，無法新增"));
        }
    }

    @Nested
    @DisplayName("PATCH /crawler-reviews/{id}")
    class UpdateCrawlerReviewTests {

        @Test
        @DisplayName("完整修改資料應正確傳入 Service")
        void validRequest() throws Exception {
            String json = """
                    {
                      "crawlerType": "BENEFIT",
                      "entityId": 88,
                      "fieldName": "benefitName",
                      "crawlerValue": "新特典",
                      "previousValue": "舊特典",
                      "reviewStatus": "RESUMED",
                      "crawledAt": "2026-11-01T01:02:03",
                      "reviewedAt": "2026-11-02T04:05:06",
                      "unlockedAt": "2026-11-03T07:08:09"
                    }
                    """;

            mockMvc.perform(patch("/crawler-reviews/7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CrawlerReviewRequest> captor = ArgumentCaptor.forClass(CrawlerReviewRequest.class);
            verify(crawlerReviewsService).updateCrawlerReview(eq(7), captor.capture());
            CrawlerReviewRequest request = captor.getValue();

            assertEquals(CinemaResourceType.BENEFIT, request.crawlerType());
            assertEquals(88, request.entityId());
            assertEquals("benefitName", request.fieldName());
            assertEquals("新特典", request.crawlerValue());
            assertEquals("舊特典", request.previousValue());
            assertEquals(ReviewStatus.RESUMED, request.reviewStatus());
            assertEquals(LocalDateTime.of(2026, 11, 1, 1, 2, 3), request.crawledAt());
            assertEquals(LocalDateTime.of(2026, 11, 2, 4, 5, 6), request.reviewedAt());
            assertEquals(LocalDateTime.of(2026, 11, 3, 7, 8, 9), request.unlockedAt());
        }

        @Test
        @DisplayName("只修改單一欄位時，其餘欄位應為 null")
        void partialRequest() throws Exception {
            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"crawlerValue\":\"只有爬蟲值改掉\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<CrawlerReviewRequest> captor = ArgumentCaptor.forClass(CrawlerReviewRequest.class);
            verify(crawlerReviewsService).updateCrawlerReview(eq(10), captor.capture());
            CrawlerReviewRequest request = captor.getValue();

            assertEquals("只有爬蟲值改掉", request.crawlerValue());
            assertNull(request.crawlerType());
            assertNull(request.entityId());
            assertNull(request.fieldName());
            assertNull(request.previousValue());
            assertNull(request.reviewStatus());
            assertNull(request.crawledAt());
            assertNull(request.reviewedAt());
            assertNull(request.unlockedAt());
        }

        @Test
        @DisplayName("空物件應允許部分修改")
        void emptyBody() throws Exception {
            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());

            verify(crawlerReviewsService).updateCrawlerReview(eq(10), any(CrawlerReviewRequest.class));
        }

        @Test
        @DisplayName("不存在的審核應回傳 404")
        void notFound() throws Exception {
            when(crawlerReviewsService.updateCrawlerReview(eq(999), any(CrawlerReviewRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "找不到爬蟲審核 ID：999"));

            mockMvc.perform(patch("/crawler-reviews/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"crawlerValue\":\"修改\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("找不到爬蟲審核 ID：999"));
        }

        @Test
        @DisplayName("Service 拒絕鎖定同步時應回傳 400")
        void lockedStatus() throws Exception {
            when(crawlerReviewsService.updateCrawlerReview(eq(10), any(CrawlerReviewRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "已解鎖的審核無法修改"));

            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reviewStatus\":\"LOCKED\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("已解鎖的審核無法修改"));

            ArgumentCaptor<CrawlerReviewRequest> captor = ArgumentCaptor.forClass(CrawlerReviewRequest.class);
            verify(crawlerReviewsService).updateCrawlerReview(eq(10), captor.capture());
            assertEquals(ReviewStatus.LOCKED, captor.getValue().reviewStatus());
        }

        @Test
        @DisplayName("ID 不是整數應回傳 400")
        void invalidId() throws Exception {
            mockMvc.perform(patch("/crawler-reviews/abc")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"crawlerValue\":\"測試\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("未提供 RequestBody 應回傳 400")
        void noBody() throws Exception {
            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(crawlerReviewsService, never()).updateCrawlerReview(any(), any());
        }

        @Test
        @DisplayName("不完整 JSON 應回傳 400")
        void invalidJson() throws Exception {
            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"crawlerValue\":\"測試\","))
                    .andExpect(status().isBadRequest());

            verify(crawlerReviewsService, never()).updateCrawlerReview(any(), any());
        }

        @Test
        @DisplayName("Service 拒絕空白欄位名稱時應回傳 400")
        void blankFieldName() throws Exception {
            when(crawlerReviewsService.updateCrawlerReview(eq(10), any(CrawlerReviewRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "欄位名稱不可為空"));

            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fieldName\":\" \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("欄位名稱不可為空"));

            verify(crawlerReviewsService).updateCrawlerReview(eq(10), any(CrawlerReviewRequest.class));
        }

        @Test
        @DisplayName("原值超過 512 字元應回傳 400")
        void previousValueTooLong() throws Exception {
            String json = """
                    {"previousValue":"%s"}
                    """.formatted("a".repeat(513));

            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("原值長度不可超過512字元"));

            verify(crawlerReviewsService, never()).updateCrawlerReview(any(), any());
        }

        @Test
        @DisplayName("日期時間格式無效應回傳 400")
        void invalidDateTime() throws Exception {
            mockMvc.perform(patch("/crawler-reviews/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reviewedAt\":\"ABC\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("日期時間格式錯誤，應為 yyyy-MM-dd HH:mm:ss"));

            verify(crawlerReviewsService, never()).updateCrawlerReview(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /crawler-reviews/{id}")
    class DeleteCrawlerReviewTests {

        @Test
        @DisplayName("正常 ID 應傳入 Service 並回傳 204")
        void validId() throws Exception {
            mockMvc.perform(delete("/crawler-reviews/7"))
                    .andExpect(status().isNoContent());

            verify(crawlerReviewsService).deleteCrawlerReview(7);
        }

        @Test
        @DisplayName("找不到審核應回傳 404")
        void notFound() throws Exception {
            doThrow(new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "找不到爬蟲審核 ID：999"))
                    .when(crawlerReviewsService).deleteCrawlerReview(999);

            mockMvc.perform(delete("/crawler-reviews/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("找不到爬蟲審核 ID：999"));
        }

        @Test
        @DisplayName("ID 不是整數應回傳 400")
        void invalidId() throws Exception {
            mockMvc.perform(delete("/crawler-reviews/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(crawlerReviewsService);
        }
    }

    @Nested
    @DisplayName("不支援的 HTTP Method")
    class HttpMethodTests {

        @Test
        @DisplayName("PUT /crawler-reviews 應回傳 405")
        void putReviews() throws Exception {
            mockMvc.perform(put("/crawler-reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405))
                    .andExpect(jsonPath("$.error").value("Method Not Allowed"));
        }

        @Test
        @DisplayName("GET /crawler-reviews/{id} 應回傳 405")
        void getReviewById() throws Exception {
            mockMvc.perform(get("/crawler-reviews/1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("POST /crawler-reviews/{id} 應回傳 405")
        void postReviewWithId() throws Exception {
            mockMvc.perform(post("/crawler-reviews/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE /crawler-reviews 應回傳 405")
        void deleteReviews() throws Exception {
            mockMvc.perform(delete("/crawler-reviews"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("未預期的程式錯誤")
    class UnexpectedErrorTests {

        @Test
        @DisplayName("Service 丟出未預期例外時回傳 500")
        void unexpectedError() throws Exception {
            when(crawlerReviewsService.findAll(
                    isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID),
                    eq(SortOrder.ASC)))
                    .thenThrow(new IllegalStateException("unexpected"));

            mockMvc.perform(get("/crawler-reviews"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("系統發生錯誤"));
        }
    }

    @Nested
    @DisplayName("JWT 權限測試")
    class JwtTests {
        private JsonWebTokenFilter filter;
        private JsonWebTokenUtility utility;
        private FilterChain chain;

        @BeforeEach
        void setUp() {
            utility = mock(JsonWebTokenUtility.class);
            chain = mock(FilterChain.class);
            filter = new JsonWebTokenFilter(utility);
        }

        @Test
        @DisplayName("GET /crawler-reviews 沒有 Token 應回傳 403")
        void getReviewsWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("GET", "/crawler-reviews");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(403, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("POST /crawler-reviews 沒有 Token 應回傳 403")
        void postReviewWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/crawler-reviews");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(403, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("PATCH /crawler-reviews/{id} 沒有 Token 應回傳 403")
        void patchReviewWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("PATCH", "/crawler-reviews/1");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(403, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("DELETE /crawler-reviews/{id} 沒有 Token 應回傳 403")
        void deleteReviewWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("DELETE", "/crawler-reviews/1");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(403, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("POST /crawler-reviews 使用錯誤 Token 應回傳 403")
        void postReviewInvalidToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/crawler-reviews");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "Bearer invalid-token");
            when(utility.validateToken("invalid-token")).thenReturn(null);

            filter.doFilter(request, response, chain);

            assertEquals(403, response.getStatus());
            verify(chain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("POST /crawler-reviews 使用有效 Token 應放行並設定 usersId")
        void postReviewValidToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/crawler-reviews");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "Bearer valid-token");
            when(utility.validateToken("valid-token")).thenReturn("123");

            filter.doFilter(request, response, chain);

            assertEquals(123, request.getAttribute("usersId"));
            verify(chain).doFilter(request, response);
        }
    }

    private CrawlerReviewResponse reviewResponse() {
        return new CrawlerReviewResponse(
                7,
                CinemaResourceType.MOVIE,
                12,
                "movieName",
                "新片名",
                "舊片名",
                ReviewStatus.PENDING,
                LocalDateTime.of(2026, 10, 8, 10, 33),
                null,
                null,
                null,
                null);
    }
}
