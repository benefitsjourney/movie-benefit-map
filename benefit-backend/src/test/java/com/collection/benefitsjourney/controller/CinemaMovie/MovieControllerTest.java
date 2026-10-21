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

import java.time.LocalDateTime;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieRequest;
import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;
import com.collection.benefitsjourney.jwt.JsonWebTokenUtility;
import com.collection.benefitsjourney.repository.UsersRepository;
import com.collection.benefitsjourney.enums.CinemaMovie.MovieSort;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.controller.AdminRequestConfig;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.service.CinemaMovie.MovieService;
import com.collection.benefitsjourney.service.UsersService;

@WebMvcTest(controllers = MovieController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JsonWebTokenFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminRequestConfig.class)
class MovieControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private MovieService movieService;
    @MockitoBean
    private UsersService usersService;

    @BeforeEach
    void setUpAdmin() {
        AdminRequestConfig.stubAdmin(usersService);
    }
    @Nested
    @DisplayName("GET /movies")
    class FindAllTests {
        @Test
        @DisplayName("沒有參數時應使用預設值")
        void findAll_DefaultParameters_ShouldReturn200() throws Exception {
            mockMvc.perform(get("/movies"))
                    .andExpect(status().isOk());
            verify(movieService).findAll(
                    null,
                    null,
                    1,
                    24,
                    MovieSort.SortBy.MOVIE_ID,
                    SortOrder.ASC);
        }

        @Test
        @DisplayName("所有查詢參數都存在時應正確傳入 Service")
        void findAll_AllParameters_ShouldPassToService() throws Exception {
            mockMvc.perform(get("/movies")
                    .param("keyword", "鬼滅")
                    .param("isVisible", "false")
                    .param("page", "3")
                    .param("size", "20")
                    .param("orderBy", "RELEASE_DATE")
                    .param("sort", "DESC"))
                    .andExpect(status().isOk());
            verify(movieService).findAll(
                    "鬼滅",
                    false,
                    3,
                    20,
                    MovieSort.SortBy.RELEASE_DATE,
                    SortOrder.DESC);
        }

        @Test
        @DisplayName("排序欄位不存在時應回傳 400")
        void findAll_InvalidOrderBy_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/movies")
                    .param("orderBy", "MOVIE_NAME"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("orderBy 參數值無效：MOVIE_NAME"));
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("isVisible 不是布林值時應回傳 400")
        void findAll_InvalidIsVisible_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/movies")
                    .param("isVisible", "ABC"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("page 不是整數時應回傳 400")
        void findAll_InvalidPage_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/movies")
                    .param("page", "abc"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("size 不是整數時應回傳 400")
        void findAll_InvalidSize_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/movies")
                    .param("size", "abc"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("Service 發生查詢錯誤時應回傳對應狀態碼")
        void findAll_ServiceBadRequest_ShouldReturn400() throws Exception {
            when(movieService.findAll(
                    isNull(),
                    isNull(),
                    eq(1),
                    eq(24),
                    eq(MovieSort.SortBy.MOVIE_ID),
                    eq(SortOrder.ASC)))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "查詢參數錯誤"));
            mockMvc.perform(get("/movies"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("查詢參數錯誤"));
        }
    }

    @Nested
    @DisplayName("GET /movies/explore")
    class FindMovieBenefitsTests {

        @Test
        @DisplayName("未帶查詢參數時，應使用 Controller 預設值")
        void defaultParameters() throws Exception {
            mockMvc.perform(get("/movies/explore"))
                    .andExpect(status().isOk());

            verify(movieService).findMovieBenefits(
                    null, null, null,
                    1, 24,
                    MovieSort.SortBy.MOVIE_ID,
                    SortOrder.ASC);
        }

        @Test
        @DisplayName("關鍵字、顯示狀態與有特典篩選應傳入探索 Service")
        void filters() throws Exception {
            mockMvc.perform(get("/movies/explore")
                    .param("keyword", "鬼滅")
                    .param("isVisible", "true")
                    .param("hasBenefit", "true")
                    .param("page", "2")
                    .param("size", "30")
                    .param("orderBy", "RELEASE_DATE")
                    .param("sort", "DESC"))
                    .andExpect(status().isOk());

            verify(movieService).findMovieBenefits(
                    "鬼滅", true, true,
                    2, 30,
                    MovieSort.SortBy.RELEASE_DATE,
                    SortOrder.DESC);
        }
    }

    @Nested
    @DisplayName("GET /movies/{id}")
    class FindByIdTests {
        @Test
        @DisplayName("正常 ID 應正確傳入 Service")
        void findById_ValidId_ShouldReturn200() throws Exception {
            mockMvc.perform(get("/movies/1"))
                    .andExpect(status().isOk());
            verify(movieService).findById(1);
        }

        @Test
        @DisplayName("ID 不是數字時應回傳 400")
        void findById_InvalidId_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/movies/abc"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("找不到電影時應回傳 404")
        void findById_NotFound_ShouldReturn404() throws Exception {
            when(movieService.findById(999))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "找不到電影 ID：999"));
            mockMvc.perform(get("/movies/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message")
                            .value("找不到電影 ID：999"));
            verify(movieService).findById(999);
        }
    }

    @Nested
    @DisplayName("POST /movies")
    class CreateMovieTests {
        @Test
        @DisplayName("完整資料應正確轉換成 MovieRequest")
        void createMovie_ValidRequest_ShouldReturn200() throws Exception {
            String json = """
                    {
                        "sourceId": "atmovies-m001",
                        "movieName": "鬼滅之刃 無限城篇",
                        "imageUrl": "https://example.com/movie.jpg",
                        "releaseDate": "2026-10-01T00:00:00",
                        "isVisible": false
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .createMovie(captor.capture());
            MovieRequest request = captor.getValue();
            assertEquals("atmovies-m001", request.sourceId());
            assertEquals(
                    "鬼滅之刃 無限城篇",
                    request.movieName());
            assertEquals(
                    "https://example.com/movie.jpg",
                    request.imageUrl());
            assertEquals(
                    LocalDateTime.of(2026, 10, 1, 0, 0),
                    request.releaseDate());
            assertFalse(request.isVisible());
        }

        @Test
        @DisplayName("沒有提供的非必要欄位應為 null")
        void createMovie_MinimalRequest_ShouldReturn200() throws Exception {
            String json = """
                    {
                        "movieName": "測試電影"
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .createMovie(captor.capture());
            MovieRequest request = captor.getValue();
            assertEquals(
                    "測試電影",
                    request.movieName());
            assertNull(request.sourceId());
            assertNull(request.imageUrl());
            assertNull(request.releaseDate());
            assertNull(request.isVisible());
        }

        @Test
        @DisplayName("isVisible 為 true 時應正確轉換")
        void createMovie_Visible_ShouldPassToService() throws Exception {
            String json = """
                    {
                        "movieName": "測試電影",
                        "isVisible": true
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .createMovie(captor.capture());
            MovieRequest request = captor.getValue();
            assertEquals(
                    "測試電影",
                    request.movieName());
            assertTrue(request.isVisible());
        }

        @Test
        @DisplayName("沒有 RequestBody 應回傳 400")
        void createMovie_NoBody_ShouldReturn400() throws Exception {
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .createMovie(any());
        }

        @Test
        @DisplayName("JSON 格式錯誤應回傳 400")
        void createMovie_InvalidJson_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "movieName": "測試電影"
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .createMovie(any());
        }

        @Test
        @DisplayName("RequestBody 型別錯誤應回傳 400")
        void createMovie_InvalidFieldType_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "movieName": "測試電影",
                        "releaseDate": {}
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .createMovie(any());
        }

        @Test
        @DisplayName("上映日期格式錯誤應回傳 400")
        void createMovie_InvalidReleaseDate_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "movieName": "測試電影",
                        "releaseDate": "ABC"
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("日期時間格式錯誤，應為 yyyy-MM-dd HH:mm:ss"));
            verify(movieService, never())
                    .createMovie(any());
        }

        @Test
        @DisplayName("電影名稱空白時應回傳 400")
        void createMovie_BlankName_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "movieName": " "
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .createMovie(any());
        }

        @Test
        @DisplayName("沒有電影名稱時應回傳 400")
        void createMovie_NoName_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "imageUrl": "https://example.com/movie.jpg"
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .createMovie(any());
        }

        @Test
        @DisplayName("沒有 application/json Content-Type 應回傳 415")
        void createMovie_NoContentType_ShouldReturn415() throws Exception {
            String json = """
                    {
                        "movieName": "測試電影"
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .content(json))
                    .andExpect(status().isUnsupportedMediaType());
            verify(movieService, never())
                    .createMovie(any());
        }

        @Test
        @DisplayName("sourceId 重複時應回傳 409")
        void createMovie_DuplicateSourceId_ShouldReturn409() throws Exception {
            when(movieService.createMovie(any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "來源 ID 已存在：M001"));
            String json = """
                    {
                        "sourceId": "M001",
                        "movieName": "測試電影"
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Service 判定請求內容錯誤時應回傳 400")
        void createMovie_ServiceBadRequest_ShouldReturn400() throws Exception {
            when(movieService.createMovie(any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "電影名稱不可為空"));
            String json = """
                    {
                        "movieName": "測試電影"
                    }
                    """;
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message")
                            .value("電影名稱不可為空"));
        }
    }

    @Nested
    @DisplayName("PATCH /movies/{id}")
    class UpdateMovieTests {
        @Test
        @DisplayName("完整修改應正確傳入 ID 與 MovieRequest")
        void updateMovie_ValidRequest_ShouldReturn200() throws Exception {
            String json = """
                    {
                        "sourceId": "M002",
                        "movieName": "修改後電影",
                        "imageUrl": "https://example.com/updated.jpg",
                        "releaseDate": "2026-11-01T00:00:00",
                        "isVisible": true
                    }
                    """;
            mockMvc.perform(patch("/movies/100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .updateMovie(
                            eq(100),
                            captor.capture());
            MovieRequest request = captor.getValue();
            assertEquals("M002", request.sourceId());
            assertEquals(
                    "修改後電影",
                    request.movieName());
            assertEquals(
                    "https://example.com/updated.jpg",
                    request.imageUrl());
            assertEquals(
                    LocalDateTime.of(2026, 11, 1, 0, 0),
                    request.releaseDate());
            assertTrue(request.isVisible());
        }

        @Test
        @DisplayName("只修改單一欄位時其他欄位應為 null")
        void updateMovie_PartialRequest_ShouldReturn200() throws Exception {
            String json = """
                    {
                        "movieName": "只有名字改掉"
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .updateMovie(
                            eq(10),
                            captor.capture());
            MovieRequest request = captor.getValue();
            assertEquals(
                    "只有名字改掉",
                    request.movieName());
            assertNull(request.sourceId());
            assertNull(request.imageUrl());
            assertNull(request.releaseDate());
            assertNull(request.isVisible());
        }

        @Test
        @DisplayName("只修改圖片網址時其他欄位應為 null")
        void updateMovie_ImageUrlOnly_ShouldReturn200() throws Exception {
            String json = """
                    {
                        "imageUrl": "https://example.com/new.jpg"
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .updateMovie(
                            eq(10),
                            captor.capture());
            MovieRequest request = captor.getValue();
            assertNull(request.sourceId());
            assertNull(request.movieName());
            assertEquals(
                    "https://example.com/new.jpg",
                    request.imageUrl());
            assertNull(request.releaseDate());
            assertNull(request.isVisible());
        }

        @Test
        @DisplayName("只修改上映日期時其他欄位應為 null")
        void updateMovie_ReleaseDateOnly_ShouldReturn200() throws Exception {
            String json = """
                    {
                        "releaseDate": "2026-12-01T00:00:00"
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .updateMovie(
                            eq(10),
                            captor.capture());
            MovieRequest request = captor.getValue();
            assertNull(request.sourceId());
            assertNull(request.movieName());
            assertNull(request.imageUrl());
            assertEquals(
                    LocalDateTime.of(2026, 12, 1, 0, 0),
                    request.releaseDate());
            assertNull(request.isVisible());
        }

        @Test
        @DisplayName("只修改顯示狀態時其他欄位應為 null")
        void updateMovie_VisibleOnly_ShouldReturn200() throws Exception {
            String json = """
                    {
                        "isVisible": false
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .updateMovie(
                            eq(10),
                            captor.capture());
            MovieRequest request = captor.getValue();
            assertNull(request.sourceId());
            assertNull(request.movieName());
            assertNull(request.imageUrl());
            assertNull(request.releaseDate());
            assertFalse(request.isVisible());
        }

        @Test
        @DisplayName("空物件應正確傳入 Service")
        void updateMovie_EmptyRequest_ShouldReturn200() throws Exception {
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService)
                    .updateMovie(
                            eq(10),
                            captor.capture());
            MovieRequest request = captor.getValue();
            assertNull(request.sourceId());
            assertNull(request.movieName());
            assertNull(request.imageUrl());
            assertNull(request.releaseDate());
            assertNull(request.isVisible());
        }

        @Test
        @DisplayName("不存在的電影應回傳 404")
        void updateMovie_NotFound_ShouldReturn404() throws Exception {
            when(movieService.updateMovie(
                    eq(999),
                    any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "找不到電影 ID：999"));
            mockMvc.perform(patch("/movies/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "movieName": "修改"
                            }
                            """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message")
                            .value("找不到電影 ID：999"));
            verify(movieService)
                    .updateMovie(
                            eq(999),
                            any());
        }

        @Test
        @DisplayName("sourceId 與其他電影重複時應回傳 409")
        void updateMovie_DuplicateSourceId_ShouldReturn409() throws Exception {
            when(movieService.updateMovie(eq(10), any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "來源 ID 已存在：M001"));
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "sourceId": "M001"
                            }
                            """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("ID 不是數字時應回傳 400")
        void updateMovie_InvalidId_ShouldReturn400() throws Exception {
            mockMvc.perform(patch("/movies/abc")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "movieName": "測試"
                            }
                            """))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("沒有 RequestBody 應回傳 400")
        void updateMovie_NoBody_ShouldReturn400() throws Exception {
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .updateMovie(any(), any());
        }

        @Test
        @DisplayName("JSON 格式錯誤應回傳 400")
        void updateMovie_InvalidJson_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "movieName": "修改後電影"
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .updateMovie(any(), any());
        }

        @Test
        @DisplayName("上映日期格式錯誤應回傳 400")
        void updateMovie_InvalidReleaseDate_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "releaseDate": "ABC"
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("日期時間格式錯誤，應為 yyyy-MM-dd HH:mm:ss"));
            verify(movieService, never())
                    .updateMovie(any(), any());
        }

        @Test
        @DisplayName("RequestBody 型別錯誤應回傳 400")
        void updateMovie_InvalidFieldType_ShouldReturn400() throws Exception {
            String json = """
                    {
                        "releaseDate": {}
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
            verify(movieService, never())
                    .updateMovie(any(), any());
        }

        @Test
        @DisplayName("Service 判定電影名稱空白時應回傳 400")
        void updateMovie_BlankName_ShouldReturn400() throws Exception {
            when(movieService.updateMovie(
                    eq(10),
                    any()))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "電影名稱不可為空"));
            String json = """
                    {
                        "movieName": " "
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message")
                            .value("電影名稱不可為空"));
        }

        @Test
        @DisplayName("沒有 application/json Content-Type 應回傳 415")
        void updateMovie_NoContentType_ShouldReturn415() throws Exception {
            String json = """
                    {
                        "movieName": "測試電影"
                    }
                    """;
            mockMvc.perform(patch("/movies/10")
                    .content(json))
                    .andExpect(status().isUnsupportedMediaType());
            verify(movieService, never())
                    .updateMovie(any(), any());
        }
    }

    @Nested
    @DisplayName("不支援的 HTTP Method")
    class HttpMethodTests {
        @Test
        @DisplayName("DELETE /movies 應回傳 405")
        void deleteMovies_ShouldReturn405() throws Exception {
            mockMvc.perform(delete("/movies"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PUT /movies/{id} 應回傳 405")
        void putMovie_ShouldReturn405() throws Exception {
            mockMvc.perform(put("/movies/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405))
                    .andExpect(jsonPath("$.error")
                            .value("Method Not Allowed"));
        }

        @Test
        @DisplayName("DELETE /movies/{id} 應回傳 405")
        void deleteMovie_ShouldReturn405() throws Exception {
            mockMvc.perform(delete("/movies/1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("POST /movies/{id} 應回傳 405")
        void postMovieWithId_ShouldReturn405() throws Exception {
            mockMvc.perform(post("/movies/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("未預期的程式錯誤")
    class UnexpectedErrorTests {
        @Test
        @DisplayName("查詢電影時 Service 丟出未預期例外應回傳 500")
        void findById_UnexpectedError_ShouldReturn500() throws Exception {
            when(movieService.findById(1))
                    .thenThrow(
                            new IllegalStateException("unexpected"));
            mockMvc.perform(get("/movies/1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error")
                            .value("Internal Server Error"))
                    .andExpect(jsonPath("$.message")
                            .value("系統發生錯誤"));
        }

        @Test
        @DisplayName("新增電影時 Service 丟出未預期例外應回傳 500")
        void createMovie_UnexpectedError_ShouldReturn500() throws Exception {
            when(movieService.createMovie(any()))
                    .thenThrow(
                            new IllegalStateException("unexpected"));
            mockMvc.perform(post("/movies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "movieName": "測試電影"
                            }
                            """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error")
                            .value("Internal Server Error"))
                    .andExpect(jsonPath("$.message")
                            .value("系統發生錯誤"));
        }

        @Test
        @DisplayName("修改電影時 Service 丟出未預期例外應回傳 500")
        void updateMovie_UnexpectedError_ShouldReturn500() throws Exception {
            when(movieService.updateMovie(
                    eq(1),
                    any()))
                    .thenThrow(
                            new IllegalStateException("unexpected"));
            mockMvc.perform(patch("/movies/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "movieName": "修改電影"
                            }
                            """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error")
                            .value("Internal Server Error"))
                    .andExpect(jsonPath("$.message")
                            .value("系統發生錯誤"));
        }
    }

    @Nested
    @DisplayName("管理員權限")
    class AdminPermissionTests {

        @Test
        @DisplayName("一般會員查詢電影清單應回傳 403")
        void findAll_NonAdmin_ShouldReturn403() throws Exception {
            UsersBean user = new UsersBean();
            user.setRole("USER");
            when(usersService.getUserById(AdminRequestConfig.ADMIN_ID)).thenReturn(user);

            mockMvc.perform(get("/movies"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value(ExceptionUtils.FORBIDDEN_MESSAGE));

            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("一般會員仍可使用電影探索")
        void explore_NonAdmin_ShouldReturn200() throws Exception {
            UsersBean user = new UsersBean();
            user.setRole("USER");
            when(usersService.getUserById(AdminRequestConfig.ADMIN_ID)).thenReturn(user);

            mockMvc.perform(get("/movies/explore"))
                    .andExpect(status().isOk());
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
        @DisplayName("GET /movies 無 Token 應回傳 401")
        void getMovies_WithoutToken_ShouldReturn401() throws Exception {
            var request = new MockHttpServletRequest("GET", "/movies");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("GET /movies/{id} 無 Token 應回傳 401")
        void getMovieById_WithoutToken_ShouldReturn401() throws Exception {
            var request = new MockHttpServletRequest("GET", "/movies/1");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, utility);
        }

        @Test
        @DisplayName("POST /movies 無 Token 應回傳 401")
        void postMovie_WithoutToken_ShouldReturn401() throws Exception {
            var request = new MockHttpServletRequest("POST", "/movies");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("PATCH /movies/{id} 無 Token 應回傳 401")
        void patchMovie_WithoutToken_ShouldReturn401() throws Exception {
            var request = new MockHttpServletRequest("PATCH", "/movies/1");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("POST /movies Token 格式錯誤應回傳 401")
        void postMovie_InvalidHeader_ShouldReturn401() throws Exception {
            var request = new MockHttpServletRequest("POST", "/movies");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "invalid-token");
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain);
            verifyNoInteractions(utility);
        }

        @Test
        @DisplayName("POST /movies 無效 Token 應回傳 401")
        void postMovie_InvalidToken_ShouldReturn401() throws Exception {
            var request = new MockHttpServletRequest("POST", "/movies");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "Bearer invalid-token");
            when(utility.validateToken("invalid-token")).thenReturn(null);
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verify(utility).validateToken("invalid-token");
            verifyNoInteractions(chain);
        }

        @Test
        @DisplayName("POST /movies 有效 Token 應放行並寫入 usersId")
        void postMovie_ValidToken_ShouldPass() throws Exception {
            var request = new MockHttpServletRequest("POST", "/movies");
            var response = new MockHttpServletResponse();
            request.addHeader("Authorization", "Bearer valid-token");
            when(utility.validateToken("valid-token")).thenReturn("123");
            UsersBean user = new UsersBean();
            user.setRole("USER");
            user.setStatus("ACTIVE");
            when(usersRepository.findById(123)).thenReturn(Optional.of(user));
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
            assertEquals(123, request.getAttribute("usersId"));
            verify(chain).doFilter(request, response);
        }
    }
}
