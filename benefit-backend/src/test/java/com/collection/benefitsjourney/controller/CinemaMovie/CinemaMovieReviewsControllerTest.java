package com.collection.benefitsjourney.controller.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.Optional;

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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaMovieReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.jwt.JsonWebTokenUtility;
import com.collection.benefitsjourney.repository.UsersRepository;
import com.collection.benefitsjourney.controller.AdminRequestConfig;
import com.collection.benefitsjourney.service.CinemaMovie.CinemaMovieReviewsService;
import com.collection.benefitsjourney.service.UsersService;

@WebMvcTest(controllers = CinemaMovieReviewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminRequestConfig.class)
class CinemaMovieReviewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CinemaMovieReviewsService cinemaMovieReviewsService;

    @MockitoBean
    private UsersService usersService;

    @BeforeEach
    void setUpAdmin() {
        AdminRequestConfig.stubAdmin(usersService);
    }

    @Nested
    @DisplayName("GET /cinema-movie/reviews")
    class FindAllTests {

        @Test
        @DisplayName("未帶查詢參數時，應使用 Controller 預設值")
        void defaultParameters() throws Exception {
            when(cinemaMovieReviewsService.findAll(
                    isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID),
                    eq(SortOrder.ASC)))
                    .thenReturn(new PageResponse<>(List.of(reviewResponse()), 1, 20, 1, 1));

            mockMvc.perform(get("/cinema-movie/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].cinemaMovieReviewId").value(7))
                    .andExpect(jsonPath("$.content[0].crawlerType").value("MOVIE"))
                    .andExpect(jsonPath("$.content[0].fieldName").value("movieName"))
                    .andExpect(jsonPath("$.content[0].reviewStatus").value("PENDING"));

            verify(cinemaMovieReviewsService).findAll(
                    null, null,
                    1, 20,
                    CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID,
                    SortOrder.ASC);
        }

        @Test
        @DisplayName("所有查詢參數應正確傳入 Service")
        void allParameters() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews")
                    .param("crawlerType", "CINEMA")
                    .param("reviewStatus", "APPROVED")
                    .param("orderBy", "UPDATED_AT")
                    .param("sort", "DESC")
                    .param("page", "3")
                    .param("size", "20"))
                    .andExpect(status().isOk());

            verify(cinemaMovieReviewsService).findAll(
                    CinemaResourceType.CINEMA,
                    ReviewStatus.APPROVED,
                    3,
                    20,
                    CinemaMovieReviewSort.SortBy.UPDATED_AT,
                    SortOrder.DESC);
        }

        @Test
        @DisplayName("無效的爬蟲類型應回傳 400")
        void invalidCrawlerType() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews").param("crawlerType", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("crawlerType 參數值無效：ABC"));

            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("無效的審查狀態應回傳 400")
        void invalidReviewStatus() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews").param("reviewStatus", "DONE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("reviewStatus 參數值無效：DONE"));

            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("無效的排序欄位應回傳 400")
        void invalidOrderBy() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews").param("orderBy", "FIELD_NAME"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("orderBy 參數值無效：FIELD_NAME"));

            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("無效的排序方向應回傳 400")
        void invalidSort() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews").param("sort", "UP"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("sort 參數值無效：UP"));

            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("page 不是整數應回傳 400")
        void invalidPage() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews").param("page", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("size 不是整數應回傳 400")
        void invalidSize() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews").param("size", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("Service 回傳 BAD_REQUEST 應對應 HTTP 400")
        void serviceBadRequest() throws Exception {
            when(cinemaMovieReviewsService.findAll(
                    isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID),
                    eq(SortOrder.ASC)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "查詢參數錯誤"));

            mockMvc.perform(get("/cinema-movie/reviews"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("查詢參數錯誤"));
        }
    }

    @Nested
    @DisplayName("POST /cinema-movie/reviews")
    class CreateCinemaMovieReviewTests {

        @Test
        @DisplayName("完整 JSON 應正確轉換為 CinemaMovieReviewRequest")
        void validRequest() throws Exception {
            String json = """
                    {
                      "crawlerType": "MOVIE",
                      "entityId": 12,
                      "fieldName": "movieName",
                      "crawlerValue": "新片名",
                      "previousValue": "舊片名",
                      "reviewStatus": "REJECTED",
                      "reviewedAt": "2026-10-09T11:00:00"
                    }
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaMovieReviewRequest> captor = ArgumentCaptor.forClass(CinemaMovieReviewRequest.class);
            verify(cinemaMovieReviewsService).createCinemaMovieReview(captor.capture());
            CinemaMovieReviewRequest request = captor.getValue();

            assertEquals(CinemaResourceType.MOVIE, request.crawlerType());
            assertEquals(12, request.entityId());
            assertEquals("movieName", request.fieldName());
            assertEquals("新片名", request.crawlerValue());
            assertEquals("舊片名", request.previousValue());
            assertEquals(ReviewStatus.REJECTED, request.reviewStatus());
            assertEquals(LocalDateTime.of(2026, 10, 9, 11, 0), request.reviewedAt());
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

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaMovieReviewRequest> captor = ArgumentCaptor.forClass(CinemaMovieReviewRequest.class);
            verify(cinemaMovieReviewsService).createCinemaMovieReview(captor.capture());
            CinemaMovieReviewRequest request = captor.getValue();

            assertEquals(CinemaResourceType.CINEMA, request.crawlerType());
            assertEquals(3, request.entityId());
            assertEquals("cinemaAddress", request.fieldName());
            assertNull(request.crawlerValue());
            assertNull(request.previousValue());
            assertNull(request.reviewStatus());
            assertNull(request.reviewedAt());
        }

        @Test
        @DisplayName("未提供 RequestBody 應回傳 400")
        void noBody() throws Exception {
            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("不完整 JSON 應回傳 400")
        void invalidJson() throws Exception {
            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"crawlerType\":\"MOVIE\","))
                    .andExpect(status().isBadRequest());

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("爬蟲類型無效應回傳 400")
        void invalidCrawlerType() throws Exception {
            String json = """
                    {"crawlerType":"ABC","entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("crawlerType 參數值無效：ABC"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("審查狀態無效應回傳 400")
        void invalidReviewStatus() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName","reviewStatus":"DONE"}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("reviewStatus 參數值無效：DONE"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("日期時間格式無效應回傳 400")
        void invalidDateTime() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName","reviewedAt":"ABC"}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("日期時間格式錯誤，應為 yyyy-MM-dd HH:mm:ss"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("未提供爬蟲類型應回傳 400")
        void missingCrawlerType() throws Exception {
            String json = """
                    {"entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("爬蟲類型不能為空"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("未提供對應資料 ID 應回傳 400")
        void missingEntityId() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("對應資料ID不能為空"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("欄位名稱為空白應回傳 400")
        void blankFieldName() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":" "}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("欄位名稱不能為空"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("欄位名稱超過 64 字元應回傳 400")
        void fieldNameTooLong() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"%s"}
                    """.formatted("a".repeat(65));

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("欄位名稱長度不可超過64字元"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("爬蟲值超過 512 字元應回傳 400")
        void crawlerValueTooLong() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName","crawlerValue":"%s"}
                    """.formatted("a".repeat(513));

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("爬蟲值長度不可超過512字元"));

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("非 application/json 應回傳 415")
        void noContentType() throws Exception {
            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews").content(json))
                    .andExpect(status().isUnsupportedMediaType());

            verify(cinemaMovieReviewsService, never()).createCinemaMovieReview(any());
        }

        @Test
        @DisplayName("已存在未接受的相同審核時應回傳 400")
        void duplicateReview() throws Exception {
            when(cinemaMovieReviewsService.createCinemaMovieReview(any(CinemaMovieReviewRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "已存在相同資料的爬蟲審核，且審查狀態不是已接受，無法新增"));

            String json = """
                    {"crawlerType":"MOVIE","entityId":1,"fieldName":"movieName"}
                    """;

            mockMvc.perform(post("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("已存在相同資料的爬蟲審核，且審查狀態不是已接受，無法新增"));
        }
    }

    @Nested
    @DisplayName("PATCH /cinema-movie/reviews/{id}/approve")
    class ApproveCinemaMovieReviewTests {

        @Test
        @DisplayName("待審核通過時應傳入審核 ID 並回傳 APPROVED")
        void approve() throws Exception {
            when(cinemaMovieReviewsService.approveCinemaMovieReview(7)).thenReturn(new CinemaMovieReviewResponse(
                    7,
                    CinemaResourceType.MOVIE,
                    12,
                    "movieName",
                    "新片名",
                    "舊片名",
                    ReviewStatus.APPROVED,
                    LocalDateTime.of(2026, 10, 19, 18, 0),
                    null,
                    null));

            mockMvc.perform(patch("/cinema-movie/reviews/7/approve"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cinemaMovieReviewId").value(7))
                    .andExpect(jsonPath("$.reviewStatus").value("APPROVED"));

            verify(cinemaMovieReviewsService).approveCinemaMovieReview(7);
        }

        @Test
        @DisplayName("不是待審核時應回傳 400")
        void alreadyReviewed() throws Exception {
            when(cinemaMovieReviewsService.approveCinemaMovieReview(7))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "你已審核過"));

            mockMvc.perform(patch("/cinema-movie/reviews/7/approve"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("你已審核過"));
        }

        @Test
        @DisplayName("找不到審核應回傳 404")
        void notFound() throws Exception {
            when(cinemaMovieReviewsService.approveCinemaMovieReview(999))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到爬蟲審核ID：999"));

            mockMvc.perform(patch("/cinema-movie/reviews/999/approve"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("找不到爬蟲審核ID：999"));
        }
    }

    @Nested
    @DisplayName("PATCH /cinema-movie/reviews/{id}/reject")
    class RejectCinemaMovieReviewTests {

        @Test
        @DisplayName("待審核拒絕時應傳入審核 ID 並回傳 REJECTED")
        void reject() throws Exception {
            when(cinemaMovieReviewsService.rejectCinemaMovieReview(7)).thenReturn(new CinemaMovieReviewResponse(
                    7,
                    CinemaResourceType.MOVIE,
                    12,
                    "movieName",
                    "新片名",
                    "舊片名",
                    ReviewStatus.REJECTED,
                    LocalDateTime.of(2026, 10, 19, 18, 0),
                    null,
                    null));

            mockMvc.perform(patch("/cinema-movie/reviews/7/reject"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cinemaMovieReviewId").value(7))
                    .andExpect(jsonPath("$.reviewStatus").value("REJECTED"));

            verify(cinemaMovieReviewsService).rejectCinemaMovieReview(7);
        }

        @Test
        @DisplayName("不是待審核時應回傳 400")
        void alreadyReviewed() throws Exception {
            when(cinemaMovieReviewsService.rejectCinemaMovieReview(7))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有待審核資料可以拒絕"));

            mockMvc.perform(patch("/cinema-movie/reviews/7/reject"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("只有待審核資料可以拒絕"));
        }

        @Test
        @DisplayName("找不到審核應回傳 404")
        void notFound() throws Exception {
            when(cinemaMovieReviewsService.rejectCinemaMovieReview(999))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到爬蟲審核ID：999"));

            mockMvc.perform(patch("/cinema-movie/reviews/999/reject"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("找不到爬蟲審核ID：999"));
        }
    }

    @Nested
    @DisplayName("不支援的 HTTP Method")
    class HttpMethodTests {

        @Test
        @DisplayName("PUT /cinema-movie/reviews 應回傳 405")
        void putReviews() throws Exception {
            mockMvc.perform(put("/cinema-movie/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405))
                    .andExpect(jsonPath("$.error").value("Method Not Allowed"));
        }

        @Test
        @DisplayName("GET /cinema-movie/reviews/{id} 應回傳 404")
        void getReviewById() throws Exception {
            mockMvc.perform(get("/cinema-movie/reviews/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /cinema-movie/reviews/{id} 應回傳 404")
        void postReviewWithId() throws Exception {
            mockMvc.perform(post("/cinema-movie/reviews/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /cinema-movie/reviews 應回傳 405")
        void deleteReviews() throws Exception {
            mockMvc.perform(delete("/cinema-movie/reviews"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("未預期的程式錯誤")
    class UnexpectedErrorTests {

        @Test
        @DisplayName("Service 丟出未預期例外時回傳 500")
        void unexpectedError() throws Exception {
            when(cinemaMovieReviewsService.findAll(
                    isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID),
                    eq(SortOrder.ASC)))
                    .thenThrow(new IllegalStateException("unexpected"));

            mockMvc.perform(get("/cinema-movie/reviews"))
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
        private UsersRepository usersRepository;
        private FilterChain chain;

        @BeforeEach
        void setUp() {
            utility = mock(JsonWebTokenUtility.class);
            usersRepository = mock(UsersRepository.class);
            chain = mock(FilterChain.class);
            filter = new JsonWebTokenFilter(utility, usersRepository);
        }

        @Test
        @DisplayName("GET /cinema-movie/reviews 沒有 Token 應回傳 401")
        void getReviewsWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("GET", "/cinema-movie/reviews");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("POST /cinema-movie/reviews 沒有 Token 應回傳 401")
        void postReviewWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/cinema-movie/reviews");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("PATCH /cinema-movie/reviews/{id}/approve 沒有 Token 應回傳 401")
        void patchReviewWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("PATCH", "/cinema-movie/reviews/1/approve");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("POST /cinema-movie/reviews 使用錯誤 Token 應回傳 401")
        void postReviewInvalidToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/cinema-movie/reviews");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "Bearer invalid-token");
            when(utility.validateToken("invalid-token")).thenReturn(null);

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verify(chain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("POST /cinema-movie/reviews 使用有效 Token 應放行並設定 usersId")
        void postReviewValidToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/cinema-movie/reviews");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "Bearer valid-token");
            when(utility.validateToken("valid-token")).thenReturn("123");
            UsersBean user = new UsersBean();
            user.setRole("USER");
            user.setStatus("ACTIVE");
            when(usersRepository.findById(123)).thenReturn(Optional.of(user));

            filter.doFilter(request, response, chain);

            assertEquals(123, request.getAttribute("usersId"));
            verify(chain).doFilter(request, response);
        }
    }

    private CinemaMovieReviewResponse reviewResponse() {
        return new CinemaMovieReviewResponse(
                7,
                CinemaResourceType.MOVIE,
                12,
                "movieName",
                "新片名",
                "舊片名",
                ReviewStatus.PENDING,
                null,
                null,
                null);
    }
}
