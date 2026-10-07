package com.collection.benefitsjourney.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.enums.CinemaSort;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.service.CinemaService;

@WebMvcTest(CinemaController.class)
class CinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CinemaService cinemaService;

    /**
     * =========================================================
     * GET /cinemas
     * =========================================================
     */

    @Nested
    @DisplayName("GET /cinemas")
    class FindAllTests {

        @Test
        @DisplayName("沒有參數時應使用預設值")
        void findAll_DefaultParameters_ShouldReturn200() throws Exception {

            mockMvc.perform(get("/cinemas"))
                    .andExpect(status().isOk());

            verify(cinemaService).findAll(
                    null,
                    null,
                    null,
                    null,
                    1,
                    10,
                    CinemaSort.SortBy.CINEMA_ID,
                    SortOrder.ASC);
        }

        @Test
        @DisplayName("所有查詢參數都存在時應正確傳入 Service")
        void findAll_AllParameters_ShouldPassToService() throws Exception {

            mockMvc.perform(get("/cinemas")
                    .param("keyword", "威秀")
                    .param("county", "TAIPEI")
                    .param("isActive", "true")
                    .param("isVisible", "false")
                    .param("page", "3")
                    .param("size", "20")
                    .param("orderBy", "CINEMA_NAME")
                    .param("sort", "DESC"))
                    .andExpect(status().isOk());

            verify(cinemaService).findAll(
                    "威秀",
                    County.TAIPEI,
                    true,
                    false,
                    3,
                    20,
                    CinemaSort.SortBy.CINEMA_NAME,
                    SortOrder.DESC);
        }

        @Test
        @DisplayName("County 不存在時應回傳 400")
        void findAll_InvalidCounty_ShouldReturn400() throws Exception {

            mockMvc.perform(get("/cinemas")
                    .param("county", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("county 參數值無效：ABC"));

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("排序欄位不存在時應回傳 400")
        void findAll_InvalidOrderBy_ShouldReturn400() throws Exception {

            mockMvc.perform(get("/cinemas")
                    .param("orderBy", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("orderBy 參數值無效：ABC"));

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("排序方向不存在時應回傳 400")
        void findAll_InvalidSort_ShouldReturn400() throws Exception {

            mockMvc.perform(get("/cinemas")
                    .param("sort", "ABC"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("sort 參數值無效：ABC"));

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("page 不是整數時應回傳 400")
        void findAll_InvalidPage_ShouldReturn400() throws Exception {

            mockMvc.perform(get("/cinemas")
                    .param("page", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("Service 發生查詢錯誤時應回傳對應狀態碼")
        void findAll_ServiceBadRequest_ShouldReturn400() throws Exception {

            when(cinemaService.findAll(
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(1),
                    eq(10),
                    eq(CinemaSort.SortBy.CINEMA_ID),
                    eq(SortOrder.ASC)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "查詢參數錯誤"));

            mockMvc.perform(get("/cinemas"))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * =========================================================
     * GET /cinemas/{id}
     * =========================================================
     */

    @Nested
    @DisplayName("GET /cinemas/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("正常 ID 應正確傳入 Service")
        void findById_ValidId_ShouldReturn200() throws Exception {

            mockMvc.perform(get("/cinemas/1"))
                    .andExpect(status().isOk());

            verify(cinemaService).findById(1);
        }

        @Test
        @DisplayName("ID 不是數字時應回傳 400")
        void findById_InvalidId_ShouldReturn400() throws Exception {

            mockMvc.perform(get("/cinemas/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("找不到影城時應回傳 404")
        void findById_NotFound_ShouldReturn404() throws Exception {

            when(cinemaService.findById(999))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "找不到影城 ID：999"));

            mockMvc.perform(get("/cinemas/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("找不到影城 ID：999"));

            verify(cinemaService).findById(999);
        }
    }

    /**
     * =========================================================
     * POST /cinemas
     * =========================================================
     */

    @Nested
    @DisplayName("POST /cinemas")
    class CreateCinemaTests {

        @Test
        @DisplayName("完整資料應正確轉換成 CinemaRequest")
        void createCinema_ValidRequest_ShouldReturn200() throws Exception {

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
            assertEquals(
                    "台北市大同區市民大道一段209號",
                    request.cinemaAddress());

            assertEquals(
                    0,
                    new BigDecimal("25.048785")
                            .compareTo(request.latitude()));

            assertEquals(
                    0,
                    new BigDecimal("121.518569")
                            .compareTo(request.longitude()));

            assertEquals(
                    "https://example.com",
                    request.officialWebsiteUrl());

            assertTrue(request.isActive());
            assertFalse(request.isVisible());
        }

        @Test
        @DisplayName("沒有提供的非必要欄位應為 null")
        void createCinema_MinimalRequest_ShouldReturn200() throws Exception {

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
        @DisplayName("沒有 RequestBody 應回傳 400")
        void createCinema_NoBody_ShouldReturn400() throws Exception {

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never())
                    .createCinema(any());
        }

        @Test
        @DisplayName("JSON 格式錯誤應回傳 400")
        void createCinema_InvalidJson_ShouldReturn400() throws Exception {

            String json = """
                    {
                        "cinemaName": "測試影城",
                        "county": "TAIPEI"
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never())
                    .createCinema(any());
        }

        @Test
        @DisplayName("County Enum 不存在時應回傳 400")
        void createCinema_InvalidCountyEnum_ShouldReturn400() throws Exception {

            String json = """
                    {
                        "cinemaName": "測試影城",
                        "county": "ABC"
                    }
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("county 參數值無效：ABC"));

            verify(cinemaService, never())
                    .createCinema(any());
        }

        @Test
        @DisplayName("RequestBody 型別錯誤應回傳 400")
        void createCinema_InvalidFieldType_ShouldReturn400() throws Exception {

            String json = """
                    {
                        "cinemaName": "測試影城",
                        "county": "TAIPEI",
                        "latitude": "hello"
                    }
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never())
                    .createCinema(any());
        }

        @Test
        @DisplayName("必填欄位空白時應回傳 400")
        void createCinema_BlankName_ShouldReturn400() throws Exception {

            String json = """
                    {
                        "cinemaName": " ",
                        "county": "TAIPEI"
                    }
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("影城名稱不能為空"));

            verify(cinemaService, never())
                    .createCinema(any());
        }

        @Test
        @DisplayName("沒有 application/json Content-Type 應回傳 415")
        void createCinema_NoContentType_ShouldReturn415() throws Exception {

            String json = """
                    {
                        "cinemaName": "測試影城",
                        "county": "TAIPEI"
                    }
                    """;

            mockMvc.perform(post("/cinemas")
                    .content(json))
                    .andExpect(status().isUnsupportedMediaType());

            verify(cinemaService, never())
                    .createCinema(any());
        }

        @Test
        @DisplayName("sourceId 重複時應回傳 409")
        void createCinema_DuplicateSourceId_ShouldReturn409() throws Exception {

            when(cinemaService.createCinema(any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "來源 ID 已存在：A001"));

            String json = """
                    {
                        "sourceId": "A001",
                        "cinemaName": "測試影城",
                        "county": "TAIPEI"
                    }
                    """;

            mockMvc.perform(post("/cinemas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isConflict());
        }
    }

    /**
     * =========================================================
     * PATCH /cinemas/{id}
     * =========================================================
     */

    @Nested
    @DisplayName("PATCH /cinemas/{id}")
    class UpdateCinemaTests {

        @Test
        @DisplayName("完整修改應正確傳入 ID 與 CinemaRequest")
        void updateCinema_ValidRequest_ShouldReturn200() throws Exception {

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

            verify(cinemaService)
                    .updateCinema(eq(100), captor.capture());

            CinemaRequest request = captor.getValue();

            assertEquals("A002", request.sourceId());
            assertEquals("修改後影城", request.cinemaName());
            assertEquals(County.NEW_TAIPEI, request.county());
            assertFalse(request.isActive());
            assertTrue(request.isVisible());
        }

        @Test
        @DisplayName("只修改單一欄位時其他欄位應為 null")
        void updateCinema_PartialRequest_ShouldReturn200() throws Exception {

            String json = """
                    {
                        "cinemaName": "只有名字改掉"
                    }
                    """;

            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);

            verify(cinemaService)
                    .updateCinema(eq(10), captor.capture());

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
        void updateCinema_NotFound_ShouldReturn404() throws Exception {

            when(cinemaService.updateCinema(eq(999), any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "找不到影城 ID：999"));

            mockMvc.perform(patch("/cinemas/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "cinemaName": "修改"
                            }
                            """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("sourceId 與其他影城重複時應回傳 409")
        void updateCinema_DuplicateSourceId_ShouldReturn409() throws Exception {

            when(cinemaService.updateCinema(eq(10), any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "來源 ID 已存在：A001"));

            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "sourceId": "A001"
                            }
                            """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("ID 不是數字時應回傳 400")
        void updateCinema_InvalidId_ShouldReturn400() throws Exception {

            mockMvc.perform(patch("/cinemas/abc")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "cinemaName": "測試"
                            }
                            """))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("沒有 RequestBody 應回傳 400")
        void updateCinema_NoBody_ShouldReturn400() throws Exception {

            mockMvc.perform(patch("/cinemas/10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(cinemaService, never())
                    .updateCinema(any(), any());
        }
    }

    /**
     * =========================================================
     * GET /cinemas/nearby
     * =========================================================
     */

    @Nested
    @DisplayName("GET /cinemas/nearby")
    class FindNearbyTests {

        @Test
        @DisplayName("沒有參數時應使用預設座標與半徑")
        void findNearby_DefaultParameters_ShouldReturn200() throws Exception {

            mockMvc.perform(get("/cinemas/nearby"))
                    .andExpect(status().isOk());

            verify(cinemaService)
                    .findNearby(
                            25.0480,
                            121.5170,
                            20000);
        }

        @Test
        @DisplayName("自訂座標與 radius 應正確傳入 Service")
        void findNearby_CustomParameters_ShouldPassToService() throws Exception {

            mockMvc.perform(get("/cinemas/nearby")
                    .param("latitude", "25.0330")
                    .param("longitude", "121.5654")
                    .param("radius", "5000"))
                    .andExpect(status().isOk());

            verify(cinemaService)
                    .findNearby(
                            25.0330,
                            121.5654,
                            5000);
        }

        @Test
        @DisplayName("/nearby 應使用附近影城 API")
        void findNearby_ShouldNotBeHandledByFindById() throws Exception {

            mockMvc.perform(get("/cinemas/nearby"))
                    .andExpect(status().isOk());

            verify(cinemaService)
                    .findNearby(
                            25.0480,
                            121.5170,
                            20000);

            verify(cinemaService, never())
                    .findById(any());
        }

        @Test
        @DisplayName("座標不是數字時應回傳 400")
        void findNearby_InvalidLatitude_ShouldReturn400() throws Exception {

            mockMvc.perform(get("/cinemas/nearby")
                    .param("latitude", "台北"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("Service 判定搜尋參數錯誤時應回傳 400")
        void findNearby_ServiceBadRequest_ShouldReturn400() throws Exception {

            when(cinemaService.findNearby(
                    25.0480,
                    121.5170,
                    -1))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "radius 不可小於 0"));

            mockMvc.perform(get("/cinemas/nearby")
                    .param("radius", "-1"))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * =========================================================
     * HTTP Method
     * =========================================================
     */

    @Nested
    @DisplayName("不支援的 HTTP Method")
    class HttpMethodTests {

        @Test
        @DisplayName("DELETE /cinemas 應回傳 405")
        void deleteCinemas_ShouldReturn405() throws Exception {

            mockMvc.perform(delete("/cinemas"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PUT /cinemas/{id} 應回傳 405")
        void putCinema_ShouldReturn405() throws Exception {

            mockMvc.perform(put("/cinemas/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405))
                    .andExpect(jsonPath("$.error").value("Method Not Allowed"));
        }

        @Test
        @DisplayName("DELETE /cinemas/{id} 應回傳 405")
        void deleteCinema_ShouldReturn405() throws Exception {

            mockMvc.perform(delete("/cinemas/1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("POST /cinemas/{id} 應回傳 405")
        void postCinemaWithId_ShouldReturn405() throws Exception {

            mockMvc.perform(post("/cinemas/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    /**
     * =========================================================
     * 未預期錯誤
     * =========================================================
     */

    @Nested
    @DisplayName("未預期的程式錯誤")
    class UnexpectedErrorTests {

        @Test
        @DisplayName("Service 丟出未預期例外時應回傳 500")
        void findById_UnexpectedError_ShouldReturn500() throws Exception {

            when(cinemaService.findById(1))
                    .thenThrow(new IllegalStateException("unexpected"));

            mockMvc.perform(get("/cinemas/1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("系統發生錯誤"));
        }
    }
}