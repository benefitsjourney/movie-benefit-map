package com.collection.benefitsjourney.controller.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import java.math.BigDecimal;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMapResponse;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaRequest;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaSort;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.jwt.JsonWebTokenUtility;
import com.collection.benefitsjourney.repository.UsersRepository;
import com.collection.benefitsjourney.service.CinemaMovie.CinemaMapService;
import com.collection.benefitsjourney.service.CinemaMovie.CinemaService;

@WebMvcTest(controllers = CinemaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class CinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CinemaService cinemaService;

    @MockitoBean
    private CinemaMapService cinemaMapService;

    @Nested
    @DisplayName("GET /cinemas")
    class FindAllTests {

        @Test
        @DisplayName("未帶查詢參數時，應使用 Controller 預設值")
        void defaultParameters() throws Exception {
            mockMvc.perform(get("/cinemas"))
                    .andExpect(status().isOk());

            verify(cinemaService).findAll(
                    null, null, null, null,
                    1, 20,
                    CinemaSort.SortBy.CINEMA_ID,
                    SortOrder.ASC);
        }

        @Test
        @DisplayName("所有查詢參數應正確傳入 Service")
        void allParameters() throws Exception {
            mockMvc.perform(get("/cinemas")
                    .param("keyword", "威秀")
                    .param("county", "TAIPEI")
                    .param("isActive", "true")
                    .param("isVisible", "false")
                    .param("page", "3")
                    .param("size", "20")
                    .param("orderBy", "COUNTY")
                    .param("sort", "DESC"))
                    .andExpect(status().isOk());

            verify(cinemaService).findAll(
                    "威秀", County.TAIPEI, true, false,
                    3, 20,
                    CinemaSort.SortBy.COUNTY,
                    SortOrder.DESC);
        }

        @Test
        @DisplayName("無效的縣市應回傳 400")
        void invalidCounty() throws Exception {
            mockMvc.perform(get("/cinemas").param("county", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("county 參數值無效：ABC"));

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("無效的排序欄位應回傳 400")
        void invalidOrderBy() throws Exception {
            mockMvc.perform(get("/cinemas").param("orderBy", "CINEMA_NAME"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("orderBy 參數值無效：CINEMA_NAME"));

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("page 不是整數應回傳 400")
        void invalidPage() throws Exception {
            mockMvc.perform(get("/cinemas").param("page", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("Service 回傳 BAD_REQUEST 應對應 HTTP 400")
        void serviceBadRequest() throws Exception {
            // 注意：Mockito any() 不符合 null，預設參數要用 isNull()。
            when(cinemaService.findAll(
                    isNull(), isNull(), isNull(), isNull(),
                    eq(1), eq(20),
                    eq(CinemaSort.SortBy.CINEMA_ID),
                    eq(SortOrder.ASC)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "查詢參數錯誤"));

            mockMvc.perform(get("/cinemas"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("查詢參數錯誤"));
        }
    }

    @Nested
    @DisplayName("GET /cinemas/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("正常 ID 應傳入 Service")
        void validId() throws Exception {
            mockMvc.perform(get("/cinemas/1"))
                    .andExpect(status().isOk());

            verify(cinemaService).findById(1);
        }

        @Test
        @DisplayName("ID 不是整數應回傳 400")
        void invalidId() throws Exception {
            mockMvc.perform(get("/cinemas/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("找不到影城應回傳 404")
        void notFound() throws Exception {
            when(cinemaService.findById(999))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "找不到影城 ID：999"));

            mockMvc.perform(get("/cinemas/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("找不到影城 ID：999"));

            verify(cinemaService).findById(999);
        }
    }

    @Nested
    @DisplayName("POST /cinemas")
    class CreateCinemaTests {

        @Test
        @DisplayName("完整 JSON 應正確轉換為 CinemaRequest")
        void validRequest() throws Exception {
            String json = """
                    {
                      "sourceId": "atmovies-001",
                      "cinemaName": "台北京站威秀",
                      "county": "TAIPEI",
                      "cinemaAddress": "台北市大同區市民大道一段209號",
                      "latitude": 25.048785,
                      "longitude": 121.518569,
                      "officialWebsiteUrl": "https://example.com",
                      "isActive": true,
                      "isVisible": false
                    }
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).createCinema(captor.capture());
            CinemaRequest request = captor.getValue();

            assertEquals("atmovies-001", request.sourceId());
            assertEquals("台北京站威秀", request.cinemaName());
            assertEquals(County.TAIPEI, request.county());
            assertEquals("台北市大同區市民大道一段209號", request.cinemaAddress());
            assertEquals(0, new BigDecimal("25.048785").compareTo(request.latitude()));
            assertEquals(0, new BigDecimal("121.518569").compareTo(request.longitude()));
            assertEquals("https://example.com", request.officialWebsiteUrl());
            assertTrue(request.isActive());
            assertFalse(request.isVisible());
        }

        @Test
        @DisplayName("省略的選填欄位應維持 null，由 Service 設定預設值")
        void minimalRequest() throws Exception {
            String json = """
                    {
                      "cinemaName": "測試影城",
                      "county": "TAIPEI"
                    }
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).createCinema(captor.capture());
            CinemaRequest request = captor.getValue();

            assertEquals("測試影城", request.cinemaName());
            assertEquals(County.TAIPEI, request.county());
            assertNull(request.sourceId());
            assertNull(request.cinemaAddress());
            assertNull(request.latitude());
            assertNull(request.longitude());
            assertNull(request.officialWebsiteUrl());
            assertNull(request.isActive());
            assertNull(request.isVisible());
        }

        @Test
        @DisplayName("未提供 RequestBody 應回傳 400")
        void noBody() throws Exception {
            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("不完整 JSON 應回傳 400")
        void invalidJson() throws Exception {
            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cinemaName\":\"測試影城\","))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("RequestBody 的 County Enum 無效應回傳 400")
        void invalidCounty() throws Exception {
            String json = """
                    {"cinemaName":"測試影城","county":"ABC"}
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("county 參數值無效：ABC"));

            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("經緯度型別無效應回傳 400")
        void invalidFieldType() throws Exception {
            String json = """
                    {"cinemaName":"測試影城","county":"TAIPEI","latitude":"hello"}
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("必填影城名稱為空白應回傳 400")
        void blankName() throws Exception {
            String json = """
                    {"cinemaName":" ","county":"TAIPEI"}
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("非 application/json 應回傳 415")
        void noContentType() throws Exception {
            String json = """
                    {"cinemaName":"測試影城","county":"TAIPEI"}
                    """;

            mockMvc.perform(post("/cinemas").content(json))
                    .andExpect(status().isUnsupportedMediaType());

            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("來源 ID 重複應回傳 409")
        void duplicateSourceId() throws Exception {
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.CONFLICT, "來源 ID 已存在：A001"));

            String json = """
                    {"sourceId":"A001","cinemaName":"測試影城","county":"TAIPEI"}
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /cinemas/{id}")
    class UpdateCinemaTests {

        @Test
        @DisplayName("完整修改資料應正確傳入 Service")
        void validRequest() throws Exception {
            String json = """
                    {
                      "sourceId": "A002",
                      "cinemaName": "修改後影城",
                      "county": "NEW_TAIPEI",
                      "cinemaAddress": "新北市板橋區測試路1號",
                      "latitude": 25.01,
                      "longitude": 121.46,
                      "officialWebsiteUrl": "https://example.com",
                      "isActive": false,
                      "isVisible": true
                    }
                    """;

            mockMvc.perform(patch("/cinemas/100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).updateCinema(eq(100), captor.capture());
            CinemaRequest request = captor.getValue();

            assertEquals("A002", request.sourceId());
            assertEquals("修改後影城", request.cinemaName());
            assertEquals(County.NEW_TAIPEI, request.county());
            assertFalse(request.isActive());
            assertTrue(request.isVisible());
        }

        @Test
        @DisplayName("只修改單一欄位時，其餘欄位應為 null")
        void partialRequest() throws Exception {
            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cinemaName\":\"只有名字改掉\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).updateCinema(eq(10), captor.capture());
            CinemaRequest request = captor.getValue();

            assertEquals("只有名字改掉", request.cinemaName());
            assertNull(request.sourceId());
            assertNull(request.county());
            assertNull(request.cinemaAddress());
            assertNull(request.latitude());
            assertNull(request.longitude());
            assertNull(request.officialWebsiteUrl());
            assertNull(request.isActive());
            assertNull(request.isVisible());
        }

        @Test
        @DisplayName("不存在的影城應回傳 404")
        void notFound() throws Exception {
            when(cinemaService.updateCinema(eq(999), any(CinemaRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "找不到影城ID：999"));

            mockMvc.perform(patch("/cinemas/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cinemaName\":\"修改\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("修改後的來源 ID 與其他影城重複應回傳 409")
        void duplicateSourceId() throws Exception {
            when(cinemaService.updateCinema(eq(10), any(CinemaRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.CONFLICT, "來源ID已存在：A001"));

            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sourceId\":\"A001\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("ID 不是整數應回傳 400")
        void invalidId() throws Exception {
            mockMvc.perform(patch("/cinemas/abc")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cinemaName\":\"測試\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("未提供 RequestBody 應回傳 400")
        void noBody() throws Exception {
            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never()).updateCinema(any(), any());
        }

        @Test
        @DisplayName("未提供影城名稱時應允許部分修改")
        void omittedName() throws Exception {
            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"isVisible\":false}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).updateCinema(eq(10), captor.capture());
            assertNull(captor.getValue().cinemaName());
            assertFalse(captor.getValue().isVisible());
        }

        @Test
        @DisplayName("Service 拒絕空白影城名稱時應回傳 400")
        void blankName() throws Exception {
            when(cinemaService.updateCinema(eq(10), any(CinemaRequest.class)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "影城名稱不可為空"));

            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cinemaName\":\" \"}"))
                    .andExpect(status().isBadRequest());

            verify(cinemaService).updateCinema(eq(10), any(CinemaRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /cinemas/map")
    class FindMapCinemasTests {

        @Test
        @DisplayName("未提供參數時應查詢全台影城")
        void nationwide() throws Exception {
            when(cinemaMapService.findNearby(null, null, null, null, null))
                    .thenReturn(new CinemaMapResponse(0, null, null, null, List.of()));

            mockMvc.perform(get("/cinemas/map"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.cinemas").isArray())
                    .andExpect(jsonPath("$.cinemas").isEmpty());

            verify(cinemaMapService).findNearby(null, null, null, null, null);
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("關鍵字、縣市、座標及半徑應正確傳入 MapService")
        void nearbyWithAllParameters() throws Exception {
            mockMvc.perform(get("/cinemas/map")
                    .param("keyword", "威秀")
                    .param("county", "TAIPEI")
                    .param("latitude", "25.03")
                    .param("longitude", "121.56")
                    .param("radius", "3000"))
                    .andExpect(status().isOk());

            verify(cinemaMapService).findNearby(
                    "威秀", County.TAIPEI, 25.03, 121.56, 3000.0);
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("省略半徑時應將 null 交給 MapService 套用預設值")
        void defaultRadius() throws Exception {
            mockMvc.perform(get("/cinemas/map")
                    .param("latitude", "25.03")
                    .param("longitude", "121.56"))
                    .andExpect(status().isOk());

            verify(cinemaMapService).findNearby(null, null, 25.03, 121.56, null);
        }

        @Test
        @DisplayName("無效的緯度型別應回傳 400，不呼叫 MapService")
        void invalidLatitudeFormat() throws Exception {
            mockMvc.perform(get("/cinemas/map")
                    .param("latitude", "abc")
                    .param("longitude", "121.56"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaMapService, cinemaService);
        }

        @Test
        @DisplayName("不合法的座標組合應回傳 MapService 的 400")
        void missingLongitude() throws Exception {
            when(cinemaMapService.findNearby(null, null, 25.03, null, null))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "緯度與經度必須同時提供"));

            mockMvc.perform(get("/cinemas/map").param("latitude", "25.03"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("緯度與經度必須同時提供"));

            verify(cinemaMapService).findNearby(null, null, 25.03, null, null);
        }

        @Test
        @DisplayName("無座標但指定半徑應回傳 MapService 的 400")
        void radiusWithoutCoordinates() throws Exception {
            when(cinemaMapService.findNearby(null, null, null, null, 5000.0))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "未提供座標時不可指定搜尋半徑"));

            mockMvc.perform(get("/cinemas/map").param("radius", "5000"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("未提供座標時不可指定搜尋半徑"));
        }

        @Test
        @DisplayName("無效的縣市代碼應回傳 400，不呼叫 MapService")
        void invalidCounty() throws Exception {
            mockMvc.perform(get("/cinemas/map").param("county", "ABC"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaMapService, cinemaService);
        }
    }

    @Nested
    @DisplayName("不支援的 HTTP Method")
    class HttpMethodTests {

        @Test
        void deleteCinemas() throws Exception {
            mockMvc.perform(delete("/cinemas"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        void putCinema() throws Exception {
            mockMvc.perform(put("/cinemas/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405))
                    .andExpect(jsonPath("$.error").value("Method Not Allowed"));
        }

        @Test
        void deleteCinema() throws Exception {
            mockMvc.perform(delete("/cinemas/1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        void postCinemaWithId() throws Exception {
            mockMvc.perform(post("/cinemas/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("未預期的程式錯誤")
    class UnexpectedErrorTests {

        @Test
        @DisplayName("Service 丟出未預期例外時回傳 500")
        void unexpectedError() throws Exception {
            when(cinemaService.findById(1))
                    .thenThrow(new IllegalStateException("unexpected"));

            mockMvc.perform(get("/cinemas/1"))
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
        @DisplayName("GET /cinemas 沒有 Token 應放行")
        void getCinemasWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("GET", "/cinemas");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(utility);
        }

        @Test
        @DisplayName("GET /cinemas/{id} 沒有 Token 應放行")
        void getCinemaByIdWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("GET", "/cinemas/1");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("POST /cinemas 沒有 Token 應回傳 401")
        void postCinemaWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/cinemas");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("PATCH /cinemas/{id} 沒有 Token 應回傳 401")
        void patchCinemaWithoutToken() throws Exception {
            var request = new MockHttpServletRequest("PATCH", "/cinemas/1");
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("POST /cinemas 使用錯誤 Token 應回傳 401")
        void postCinemaInvalidToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/cinemas");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "Bearer invalid-token");
            when(utility.validateToken("invalid-token")).thenReturn(null);

            filter.doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            verify(chain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("POST /cinemas 使用有效 Token 應放行並設定 usersId")
        void postCinemaValidToken() throws Exception {
            var request = new MockHttpServletRequest("POST", "/cinemas");
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
}
