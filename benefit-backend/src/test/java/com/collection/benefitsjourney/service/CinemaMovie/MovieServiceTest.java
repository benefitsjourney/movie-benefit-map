package com.collection.benefitsjourney.service.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.MovieSort;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.MovieRepository;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {
    @Mock
    private MovieRepository movieRepository;
    @InjectMocks
    private MovieService movieService;
    private MovieBean movie(Integer id, String sourceId, String name) {
        MovieBean movie = new MovieBean();
        movie.setMovieId(id);
        movie.setSourceId(sourceId);
        movie.setMovieName(name);
        movie.setIsVisible(true);
        return movie;
    }

    @Nested
    @DisplayName("findAll - 查詢、分頁與排序")
    class FindAllTests {
        @Test
        @DisplayName("查詢參數應傳到 Repository，頁碼應從 1-based 轉 0-based")
        void shouldPassFiltersAndPaging() {
            MovieBean bean = movie(1, "M001", "測試電影");
            when(movieRepository.findAllByFilters(
                    any(String.class),
                    eq(true),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(bean)));
            PageResponse<MovieResponse> result = movieService.findAll(
                    "測試", true,
                    3, 20,
                    MovieSort.SortBy.RELEASE_DATE,
                    SortOrder.DESC);
            assertNotNull(result);
            ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(movieRepository).findAllByFilters(
                    keywordCaptor.capture(),
                    eq(true),
                    pageableCaptor.capture());
            // 不綁死 LikeKeywords 內部的跳脫規則，只驗證關鍵字被保留。
            assertTrue(keywordCaptor.getValue().contains("測試"));
            Pageable pageable = pageableCaptor.getValue();
            assertEquals(2, pageable.getPageNumber());
            assertEquals(20, pageable.getPageSize());
            Sort.Order releaseDate = pageable.getSort().getOrderFor("releaseDate");
            assertNotNull(releaseDate);
            assertEquals(Sort.Direction.DESC, releaseDate.getDirection());
            assertEquals(Sort.Direction.ASC,
                    pageable.getSort().getOrderFor("movieId").getDirection());
        }

        @Test
        @DisplayName("未帶篩選時仍依 movieId 升序")
        void shouldSortByMovieIdAscending() {
            when(movieRepository.findAllByFilters(
                    nullable(String.class),
                    nullable(Boolean.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            PageResponse<MovieResponse> result = movieService.findAll(
                    null, null,
                    1, 24,
                    MovieSort.SortBy.MOVIE_ID,
                    SortOrder.ASC);
            assertNotNull(result);
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(movieRepository).findAllByFilters(
                    nullable(String.class),
                    nullable(Boolean.class),
                    captor.capture());
            Pageable pageable = captor.getValue();
            assertEquals(0, pageable.getPageNumber());
            assertEquals(24, pageable.getPageSize());
            assertEquals(
                    Sort.Direction.ASC,
                    pageable.getSort()
                            .getOrderFor("movieId")
                            .getDirection());
        }
    }

    @Nested
    @DisplayName("findById - 單筆查詢")
    class FindByIdTests {
        @Test
        @DisplayName("存在的 ID 應回傳轉換後的 MovieResponse")
        void shouldReturnMovie() {
            MovieBean bean = movie(1, "M001", "測試電影");
            when(movieRepository.findById(1))
                    .thenReturn(Optional.of(bean));
            MovieResponse result = movieService.findById(1);
            assertNotNull(result);
            verify(movieRepository).findById(1);
        }

        @Test
        @DisplayName("不存在的 ID 應回傳 404")
        void shouldThrowNotFound() {
            when(movieRepository.findById(999))
                    .thenReturn(Optional.empty());
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.findById(999));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到電影 ID：999", exception.getReason());
        }
    }

    @Nested
    @DisplayName("createMovie - 新增電影")
    class CreateMovieTests {
        @Test
        @DisplayName("Request 為 null 應回傳 400，且不呼叫 Repository")
        void nullRequest() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.createMovie(null));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("請求內容不可為空", exception.getReason());
            verifyNoInteractions(movieRepository);
        }

        @Test
        @DisplayName("名稱未填應回傳 400")
        void nullName() {
            MovieRequest request = mock(MovieRequest.class);
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.createMovie(request));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("電影名稱不可為空", exception.getReason());
            verifyNoInteractions(movieRepository);
        }

        @Test
        @DisplayName("名稱只含空白應回傳 400")
        void blankName() {
            MovieRequest request = mock(MovieRequest.class);
            when(request.movieName()).thenReturn("   ");
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.createMovie(request));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("電影名稱不可為空", exception.getReason());
            verifyNoInteractions(movieRepository);
        }

        @Test
        @DisplayName("來源 ID 與現有電影重複應回傳 409，不能儲存")
        void duplicateSourceId() {
            MovieRequest request = mock(MovieRequest.class);
            when(request.movieName()).thenReturn("測試電影");
            when(request.sourceId()).thenReturn("M001");
            when(movieRepository.findBySourceId("M001"))
                    .thenReturn(Optional.of(
                            movie(1, "M001", "既有電影")));
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.createMovie(request));
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("來源 ID 已存在：M001", exception.getReason());
            verify(movieRepository, never()).save(any(MovieBean.class));
        }

        @Test
        @DisplayName("未提供 isVisible 時，新增後應預設為 true")
        void shouldSetDefaultFlags() {
            MovieRequest request = mock(MovieRequest.class);
            when(request.movieName()).thenReturn("測試電影");
            // Mockito 的 Boolean 預設為 false，需明確模擬 JSON 未提供欄位。
            when(request.isVisible()).thenReturn((Boolean) null);
            when(movieRepository.save(any(MovieBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            MovieResponse result = movieService.createMovie(request);
            assertNotNull(result);
            ArgumentCaptor<MovieBean> captor = ArgumentCaptor.forClass(MovieBean.class);
            verify(movieRepository).save(captor.capture());
            MovieBean saved = captor.getValue();
            assertEquals("測試電影", saved.getMovieName());
            assertEquals(Boolean.TRUE, saved.getIsVisible());
            verify(movieRepository, never())
                    .findBySourceId(any(String.class));
        }

        @Test
        @DisplayName("完整 Request 欄位應正確儲存，明確傳入 false 不應被覆寫")
        void shouldSaveAllFields() {
            MovieRequest request = mock(MovieRequest.class);
            LocalDateTime releaseDate = LocalDateTime.of(2026, 10, 12, 0, 0);
            when(request.sourceId()).thenReturn("M002");
            when(request.sourceUrl()).thenReturn("https://www.atmovies.com.tw/movie/M002/");
            when(request.movieName()).thenReturn("測試電影");
            when(request.imageUrl())
                    .thenReturn("https://example.com/movie.jpg");
            when(request.releaseDate()).thenReturn(releaseDate);
            when(request.isVisible()).thenReturn(false);
            when(movieRepository.findBySourceId("M002"))
                    .thenReturn(Optional.empty());
            when(movieRepository.save(any(MovieBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            MovieResponse result = movieService.createMovie(request);
            assertNotNull(result);
            ArgumentCaptor<MovieBean> captor = ArgumentCaptor.forClass(MovieBean.class);
            verify(movieRepository).save(captor.capture());
            MovieBean saved = captor.getValue();
            assertEquals("M002", saved.getSourceId());
            assertEquals("https://www.atmovies.com.tw/movie/M002/", saved.getSourceUrl());
            assertEquals("測試電影", saved.getMovieName());
            assertEquals(
                    "https://example.com/movie.jpg",
                    saved.getImageUrl());
            assertEquals(releaseDate, saved.getReleaseDate());
            assertEquals(Boolean.FALSE, saved.getIsVisible());
        }
    }

    @Nested
    @DisplayName("updateMovie - 修改電影")
    class UpdateMovieTests {
        @Test
        @DisplayName("Request 為 null 應回傳 400")
        void nullRequest() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.updateMovie(1, null));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("請求內容不可為空", exception.getReason());
            verifyNoInteractions(movieRepository);
        }

        @Test
        @DisplayName("修改名稱為空白應回傳 400")
        void blankName() {
            MovieRequest request = mock(MovieRequest.class);
            when(request.movieName()).thenReturn(" ");
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.updateMovie(1, request));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("電影名稱不可為空", exception.getReason());
            verifyNoInteractions(movieRepository);
        }

        @Test
        @DisplayName("修改不存在的電影應回傳 404")
        void notFound() {
            MovieRequest request = mock(MovieRequest.class);
            when(movieRepository.findById(999))
                    .thenReturn(Optional.empty());
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.updateMovie(999, request));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到電影 ID：999", exception.getReason());
            verify(movieRepository, never()).save(any(MovieBean.class));
        }

        @Test
        @DisplayName("部分修改只更新帶入欄位，其餘欄位保持原值")
        void shouldOnlyUpdateProvidedFields() {
            MovieBean existing = movie(1, "M001", "原本電影");
            LocalDateTime originalDate = LocalDateTime.of(2026, 1, 1, 0, 0);
            existing.setImageUrl("https://example.com/old.jpg");
            existing.setReleaseDate(originalDate);
            existing.setIsVisible(false);
            MovieRequest request = mock(MovieRequest.class);
            when(request.movieName()).thenReturn("改名後電影");
            when(movieRepository.findById(1))
                    .thenReturn(Optional.of(existing));
            when(movieRepository.save(any(MovieBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            MovieResponse result = movieService.updateMovie(1, request);
            assertNotNull(result);
            ArgumentCaptor<MovieBean> captor = ArgumentCaptor.forClass(MovieBean.class);
            verify(movieRepository).save(captor.capture());
            MovieBean saved = captor.getValue();
            assertSame(existing, saved);
            assertEquals("改名後電影", saved.getMovieName());
            assertEquals("M001", saved.getSourceId());
            assertEquals(
                    "https://example.com/old.jpg",
                    saved.getImageUrl());
            assertEquals(originalDate, saved.getReleaseDate());
            assertEquals(Boolean.FALSE, saved.getIsVisible());
            verify(movieRepository, never())
                    .findBySourceId(any(String.class));
        }

        @Test
        @DisplayName("sourceId 和自己相同時可正常修改，不算重複")
        void sameSourceIdShouldBeAllowed() {
            MovieBean existing = movie(1, "M001", "原本電影");
            MovieRequest request = mock(MovieRequest.class);
            when(request.sourceId()).thenReturn("M001");
            when(movieRepository.findById(1))
                    .thenReturn(Optional.of(existing));
            when(movieRepository.findBySourceId("M001"))
                    .thenReturn(Optional.of(existing));
            when(movieRepository.save(any(MovieBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            MovieResponse result = movieService.updateMovie(1, request);
            assertNotNull(result);
            verify(movieRepository).save(existing);
        }

        @Test
        @DisplayName("sourceId 被另一部電影使用時應回傳 409")
        void duplicateSourceIdShouldConflict() {
            MovieBean existing = movie(1, "M001", "目前電影");
            MovieBean other = movie(2, "M002", "其他電影");
            MovieRequest request = mock(MovieRequest.class);
            when(request.sourceId()).thenReturn("M002");
            when(movieRepository.findById(1))
                    .thenReturn(Optional.of(existing));
            when(movieRepository.findBySourceId("M002"))
                    .thenReturn(Optional.of(other));
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> movieService.updateMovie(1, request));
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("來源 ID 已存在：M002", exception.getReason());
            verify(movieRepository, never()).save(any(MovieBean.class));
        }

        @Test
        @DisplayName("修改來源 ID、圖片、上映日期與顯示狀態")
        void shouldUpdateEveryOptionalField() {
            MovieBean existing = movie(1, "M001", "原本電影");
            existing.setImageUrl("https://example.com/old.jpg");
            existing.setReleaseDate(
                    LocalDateTime.of(2026, 1, 1, 0, 0));
            MovieRequest request = mock(MovieRequest.class);
            LocalDateTime newReleaseDate = LocalDateTime.of(2026, 12, 25, 0, 0);
            when(request.sourceId()).thenReturn("M003");
            when(request.sourceUrl()).thenReturn("https://www.atmovies.com.tw/movie/M003/");
            when(request.imageUrl())
                    .thenReturn("https://example.com/new.jpg");
            when(request.releaseDate()).thenReturn(newReleaseDate);
            when(request.isVisible()).thenReturn(false);
            when(movieRepository.findById(1))
                    .thenReturn(Optional.of(existing));
            when(movieRepository.findBySourceId("M003"))
                    .thenReturn(Optional.empty());
            when(movieRepository.save(any(MovieBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            MovieResponse result = movieService.updateMovie(1, request);
            assertNotNull(result);
            ArgumentCaptor<MovieBean> captor = ArgumentCaptor.forClass(MovieBean.class);
            verify(movieRepository).save(captor.capture());
            MovieBean saved = captor.getValue();
            assertSame(existing, saved);
            assertEquals("M003", saved.getSourceId());
            assertEquals("https://www.atmovies.com.tw/movie/M003/", saved.getSourceUrl());
            assertEquals("原本電影", saved.getMovieName());
            assertEquals(
                    "https://example.com/new.jpg",
                    saved.getImageUrl());
            assertEquals(newReleaseDate, saved.getReleaseDate());
            assertEquals(Boolean.FALSE, saved.getIsVisible());
        }
    }

    @Nested
    @DisplayName("findBySourceId - 根據來源 ID 查詢")
    class FindBySourceIdTests {
        @Test
        @DisplayName("sourceId 為 null 或空白應直接回傳 empty，不能查詢 Repository")
        void blankSourceId() {
            assertTrue(movieService.findBySourceId(null).isEmpty());
            assertTrue(movieService.findBySourceId("").isEmpty());
            assertTrue(movieService.findBySourceId("   ").isEmpty());
            verifyNoInteractions(movieRepository);
        }

        @Test
        @DisplayName("來源 ID 存在時應回傳 MovieResponse")
        void shouldReturnMovie() {
            when(movieRepository.findBySourceId("M001"))
                    .thenReturn(Optional.of(
                            movie(1, "M001", "測試電影")));
            Optional<MovieResponse> result = movieService.findBySourceId("M001");
            assertTrue(result.isPresent());
            verify(movieRepository).findBySourceId("M001");
        }

        @Test
        @DisplayName("來源 ID 不存在時應回傳 empty")
        void shouldReturnEmpty() {
            when(movieRepository.findBySourceId("UNKNOWN"))
                    .thenReturn(Optional.empty());
            Optional<MovieResponse> result = movieService.findBySourceId("UNKNOWN");
            assertFalse(result.isPresent());
            verify(movieRepository).findBySourceId("UNKNOWN");
        }
    }
}
