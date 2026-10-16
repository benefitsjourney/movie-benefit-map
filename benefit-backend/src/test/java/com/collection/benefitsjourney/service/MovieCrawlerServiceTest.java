package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.MovieRequest;
import com.collection.benefitsjourney.dto.MovieResponse;

/**
 * 透過 Spy connect() 模擬網站，Mockito Mock 模擬資料庫服務。
 * 前提：MovieCrawlerService 的 connect(String url) 改為 package-private（移除 private）。
 * 不能依賴 mockStatic(Jsoup.class)：電影列表是在 ExecutorService 工作執行緒下載。
 */
@ExtendWith(MockitoExtension.class)
class MovieCrawlerServiceTest {

    private static final String BASE = "https://www.atmovies.com.tw";
    private static final String NEW = BASE + "/movie/new/";
    private static final String NOW = BASE + "/movie/now/0/";
    private static final String NOW2 = BASE + "/movie/now2/0/";
    private static final String NEXT = BASE + "/movie/next/0/";
    private static final String DETAIL_A = BASE + "/movie/fA001/";
    private static final String FILM_DETAIL_A = BASE + "/film/fA001/";

    private static final LocalDateTime DATE_A = LocalDateTime.of(2026, 10, 12, 0, 0);
    private static final LocalDateTime DATE_B = LocalDateTime.of(2026, 11, 1, 0, 0);

    @Mock
    private MovieService movieService;

    @Mock
    private CrawlerReviewsService crawlerReviewsService;

    private MovieCrawlerService crawler;

    @BeforeEach
    void setUp() {
        crawler = spy(new MovieCrawlerService(movieService, crawlerReviewsService));
    }

    private Document page(String url, String html) {
        return Jsoup.parse(html, url);
    }

    private void pageAt(String url, String html) throws IOException {
        doReturn(page(url, html)).when(crawler).connect(url);
    }

    private void pages(String newHtml, String nowHtml, String now2Html, String nextHtml)
            throws IOException {
        pageAt(NEW, newHtml);
        pageAt(NOW, nowHtml);
        pageAt(NOW2, now2Html);
        pageAt(NEXT, nextHtml);
    }

    private void onlyNewPage(String newHtml) throws IOException {
        pages(newHtml, "<html></html>", "<html></html>", "<html></html>");
    }

    private String fullArticle(String sourceId, String name, String poster, String date) {
        return """
                <article class="box post">
                  <div class="filmTitle"><a href="/movie/%s/">%s</a></div>
                  <div class="filmImg"><img src="%s" /></div>
                  <p>上映日期：%s</p>
                </article>
                """.formatted(sourceId, name, poster, date);
    }

    private MovieResponse saved(int id, String sourceId, String name,
            String imageUrl, boolean visible, LocalDateTime date) {
        return new MovieResponse(id, sourceId, name, imageUrl, visible, date, null, null);
    }

    @Nested
    @DisplayName("syncMovies - 新增電影與解析")
    class CreateTests {

        @Test
        @DisplayName("列表資料完整時直接新增，不應讀取詳細頁")
        void createFromList() throws Exception {
            onlyNewPage(fullArticle("fA001", "測試電影", "/poster/a.jpg", "2026/10/12"));
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());

            MovieResponse saved = saved(1, "fA001", "測試電影",
                    BASE + "/poster/a.jpg", true, DATE_A);
            when(movieService.createMovie(any(MovieRequest.class))).thenReturn(saved);

            List<MovieResponse> result = crawler.syncMovies();

            assertEquals(List.of(saved), result);
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            MovieRequest request = captor.getValue();

            assertEquals("fA001", request.sourceId());
            assertEquals("測試電影", request.movieName());
            assertEquals(BASE + "/poster/a.jpg", request.imageUrl());
            assertEquals(Boolean.TRUE, request.isVisible());
            assertEquals(DATE_A, request.releaseDate());
            verify(crawler, never()).connect(DETAIL_A);
            verify(movieService, never()).updateMovie(anyInt(), any());
        }

        @Test
        @DisplayName("同一部電影跨列表重複出現，只應新增一次")
        void deduplicateAcrossPages() throws Exception {
            pages(fullArticle("fA001", "測試電影", "/poster/a.jpg", "2026/10/12"),
                    "<a href='/movie/fA001/'>測試電影</a>",
                    "<a href='/movie/fA001/'>測試電影</a>", "");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/a.jpg", true, DATE_A));

            List<MovieResponse> result = crawler.syncMovies();

            assertEquals(1, result.size());
            verify(movieService).findBySourceId("fA001");
            verify(movieService).createMovie(any(MovieRequest.class));
            verify(crawler, never()).connect(DETAIL_A);
        }

        @Test
        @DisplayName("列表缺少圖片與日期時，應讀取詳細頁補齊")
        void fillMissingFieldsFromDetail() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>測試電影</a>");
            pageAt(DETAIL_A, """
                    <div class="filmPic"><img src="/poster/detail.jpg"></div>
                    <p>上映日期：2026/10/12</p>
                    """);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/detail.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());

            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals(BASE + "/poster/detail.jpg", captor.getValue().imageUrl());
            assertEquals(DATE_A, captor.getValue().releaseDate());
            verify(crawler).connect(DETAIL_A);
        }

        @Test
        @DisplayName("應清除英文副標與網站後綴，並接受延遲載入圖片")
        void cleanTitleAndLazyPoster() throws Exception {
            onlyNewPage("""
                    <article class="box post">
                      <div class="filmTitle">
                        <a href="/movie/fA001/">測試電影 English Title @movies</a>
                      </div>
                      <img data-src="/poster/lazy.jpg" />
                      <p>上映日期：2026/10/12</p>
                    </article>
                    """);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/lazy.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals("測試電影", captor.getValue().movieName());
            assertEquals(BASE + "/poster/lazy.jpg", captor.getValue().imageUrl());
            assertEquals(DATE_A, captor.getValue().releaseDate());
            verify(crawler, never()).connect(DETAIL_A);
        }

        @Test
        @DisplayName("月/日/年格式的上映日期應可解析")
        void parseMonthFirstReleaseDate() throws Exception {
            onlyNewPage(fullArticle("fA001", "測試電影", "/poster/a.jpg", "10/12/2026"));
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/a.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals(DATE_A, captor.getValue().releaseDate());
        }

        @Test
        @DisplayName("詳細頁標題後綴應清除，並接受 /film/ 網址")
        void cleanDetailTitleFromFilmUrl() throws Exception {
            onlyNewPage("<a href='/film/fA001/'>開眼電影網</a>");
            pageAt(FILM_DETAIL_A, """
                    <meta property="og:title" content="另一部電影〖開眼電影網〗">
                    <meta property="og:image" content="https://img.example.com/poster.jpg">
                    <p>上映日期：2026/10/12</p>
                    """);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "另一部電影",
                            "https://img.example.com/poster.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals("另一部電影", captor.getValue().movieName());
            assertEquals("https://img.example.com/poster.jpg", captor.getValue().imageUrl());
            assertEquals(DATE_A, captor.getValue().releaseDate());
            verify(crawler).connect(FILM_DETAIL_A);
        }

        @Test
        @DisplayName("詳細頁無法連線但列表有名稱時，仍可新增部分資料")
        void detailConnectionFailed() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>測試電影</a>");
            doThrow(new IOException("detail failed")).when(crawler).connect(DETAIL_A);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影", null, true, null));

            assertEquals(1, crawler.syncMovies().size());

            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals("測試電影", captor.getValue().movieName());
            assertNull(captor.getValue().imageUrl());
            assertNull(captor.getValue().releaseDate());
        }

        @Test
        @DisplayName("詳細頁回傳 HTTP 錯誤但列表有名稱時，仍可新增部分資料")
        void detailHttpStatusFailed() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>測試電影</a>");
            doThrow(new HttpStatusException("detail failed", 404, DETAIL_A))
                    .when(crawler).connect(DETAIL_A);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影", null, true, null));

            assertEquals(1, crawler.syncMovies().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals("測試電影", captor.getValue().movieName());
            assertNull(captor.getValue().imageUrl());
            assertNull(captor.getValue().releaseDate());
        }

        @Test
        @DisplayName("無法解析的上映日期應略過，電影仍可新增")
        void unparseableReleaseDate() throws Exception {
            onlyNewPage(fullArticle("fA001", "測試電影", "/poster/a.jpg", "13/40/2026"));
            pageAt(DETAIL_A, "<html><body>沒有上映日期</body></html>");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/a.jpg", true, null));

            assertEquals(1, crawler.syncMovies().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals("測試電影", captor.getValue().movieName());
            assertEquals(BASE + "/poster/a.jpg", captor.getValue().imageUrl());
            assertNull(captor.getValue().releaseDate());
        }

        @Test
        @DisplayName("非法電影網址應被忽略，不可寫入資料庫")
        void ignoreUnrelatedLinks() throws Exception {
            onlyNewPage("""
                    <a href='/showtime/t02a01/'>影城</a>
                    <a href='/movie/new/'>本週新片</a>
                    <a href='https://example.com/movie/fA001/'>外站電影</a>
                    """);

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            verifyNoInteractions(movieService);
        }
    }

    @Nested
    @DisplayName("syncMovies - 已存在電影")
    class UpdateTests {

        @Test
        @DisplayName("電影內容完全相同時應直接回傳原資料，不需 update")
        void unchangedMovie() throws Exception {
            onlyNewPage(fullArticle("fA001", "測試電影", "/poster/a.jpg", "2026/10/12"));
            MovieResponse existing = saved(1, "fA001", "測試電影",
                    BASE + "/poster/a.jpg", false, DATE_A);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.of(existing));

            List<MovieResponse> result = crawler.syncMovies();

            assertEquals(1, result.size());
            assertSame(existing, result.get(0));
            verify(movieService, never()).updateMovie(anyInt(), any());
            verify(movieService, never()).createMovie(any());
            verify(crawler, never()).connect(DETAIL_A);
        }

        @Test
        @DisplayName("名稱不同時只送審，正式資料與未抓到的欄位都保持原樣")
        void queueNameChangeWithoutUpdatingMovie() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>更新後名稱</a>");
            MovieResponse existing = saved(1, "fA001", "舊名稱",
                    BASE + "/poster/old.jpg", false, DATE_A);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.of(existing));
            when(crawlerReviewsService.submitCrawlerChange(
                    any(), any(), any(), any(), any())).thenReturn(true);

            List<MovieResponse> result = crawler.syncMovies();

            assertEquals(List.of(existing), result);
            verify(crawlerReviewsService).submitCrawlerChange(
                    com.collection.benefitsjourney.enums.CinemaResourceType.MOVIE,
                    1,
                    CrawlerReviewsService.MOVIE_NAME,
                    "更新後名稱",
                    "舊名稱");
            verifyNoMoreInteractions(crawlerReviewsService);
            verify(movieService, never()).updateMovie(anyInt(), any());
            verify(movieService, never()).createMovie(any());
            verify(crawler, never()).connect(DETAIL_A);
        }

        @Test
        @DisplayName("列表缺少上映日期時，可由後面的列表補上")
        void mergePartialPageInformation() throws Exception {
            pages("<a href='/movie/fA001/'>測試電影</a>",
                    fullArticle("fA001", "測試電影", "/poster/a.jpg", "2026/10/12"),
                    "", "");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/a.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals(DATE_A, captor.getValue().releaseDate());
            assertEquals(BASE + "/poster/a.jpg", captor.getValue().imageUrl());
            verify(crawler, never()).connect(DETAIL_A);
        }

        @Test
        @DisplayName("先出現的片名應保留，後面列表只補空缺欄位")
        void keepEarlierNameWhenMerging() throws Exception {
            pages("<a href='/movie/fA001/'>先出現的名稱</a>",
                    fullArticle("fA001", "後出現的名稱", "/poster/a.jpg", "2026/10/12"),
                    "", "");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "先出現的名稱",
                            BASE + "/poster/a.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).createMovie(captor.capture());
            assertEquals("先出現的名稱", captor.getValue().movieName());
            assertEquals(BASE + "/poster/a.jpg", captor.getValue().imageUrl());
            assertEquals(DATE_A, captor.getValue().releaseDate());
            verify(crawler, never()).connect(DETAIL_A);
        }

        @Test
        @DisplayName("圖片與上映日期不同時只送審變更欄位")
        void queueImageAndDateChanges() throws Exception {
            onlyNewPage(fullArticle("fA001", "測試電影", "/poster/new.jpg", "2026/11/1"));
            MovieResponse existing = saved(1, "fA001", "測試電影",
                    BASE + "/poster/old.jpg", false, DATE_A);
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.of(existing));

            assertEquals(List.of(existing), crawler.syncMovies());

            verify(crawlerReviewsService).submitCrawlerChange(
                    com.collection.benefitsjourney.enums.CinemaResourceType.MOVIE,
                    1,
                    CrawlerReviewsService.IMAGE_URL,
                    BASE + "/poster/new.jpg",
                    BASE + "/poster/old.jpg");
            verify(crawlerReviewsService).submitCrawlerChange(
                    com.collection.benefitsjourney.enums.CinemaResourceType.MOVIE,
                    1,
                    CrawlerReviewsService.RELEASE_DATE,
                    "2026-11-01 00:00:00",
                    "2026-10-12 00:00:00");
            verify(crawlerReviewsService, never()).submitCrawlerChange(
                    any(), any(), eq(CrawlerReviewsService.MOVIE_NAME), any(), any());
            verify(movieService, never()).updateMovie(anyInt(), any());
            verify(crawler, never()).connect(DETAIL_A);
        }
    }

    @Nested
    @DisplayName("syncMovies - 異常與部分成功")
    class ErrorTests {

        @Test
        @DisplayName("四個電影列表都抓不到資料時應回傳 502")
        void emptyLists() throws Exception {
            onlyNewPage("<html><body>目前沒有電影</body></html>");

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("電影爬蟲未成功同步任何資料", error.getReason());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("四個電影列表全都連線失敗應回傳 502")
        void allListsFailed() throws Exception {
            for (String url : List.of(NEW, NOW, NOW2, NEXT)) {
                doThrow(new IOException("timeout")).when(crawler).connect(url);
            }

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("電影爬蟲未成功同步任何資料", error.getReason());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("部分列表失敗時仍能同步其他列表的電影")
        void partialListFailure() throws Exception {
            pageAt(NEW, fullArticle("fA001", "測試電影", "/poster/a.jpg", "2026/10/12"));
            doThrow(new IOException("second list failed")).when(crawler).connect(NOW);
            pageAt(NOW2, "");
            pageAt(NEXT, "");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/a.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());
            verify(movieService).createMovie(any(MovieRequest.class));
        }

        @Test
        @DisplayName("單一列表回傳 HTTP 錯誤時，仍能同步其他列表的電影")
        void partialListHttpStatusFailure() throws Exception {
            pageAt(NEW, fullArticle("fA001", "測試電影", "/poster/a.jpg", "2026/10/12"));
            doThrow(new HttpStatusException("upstream failed", 503, NOW))
                    .when(crawler).connect(NOW);
            pageAt(NOW2, "");
            pageAt(NEXT, "");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影",
                            BASE + "/poster/a.jpg", true, DATE_A));

            assertEquals(1, crawler.syncMovies().size());
            verify(movieService).createMovie(any(MovieRequest.class));
        }

        @Test
        @DisplayName("單筆電影查詢資料庫失敗時，應略過並繼續同步其他電影")
        void partialLookupFailure() throws Exception {
            onlyNewPage(fullArticle("fA001", "第一部", "/poster/a.jpg", "2026/10/12")
                    + fullArticle("fB002", "第二部", "/poster/b.jpg", "2026/11/1"));
            when(movieService.findBySourceId("fA001"))
                    .thenThrow(new IllegalStateException("db failed"));
            when(movieService.findBySourceId("fB002")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(2, "fB002", "第二部",
                            BASE + "/poster/b.jpg", true, DATE_B));

            List<MovieResponse> result = crawler.syncMovies();

            assertEquals(1, result.size());
            assertEquals("fB002", result.get(0).sourceId());
        }

        @Test
        @DisplayName("單筆電影儲存失敗時，應略過並繼續同步其他電影")
        void partialSaveFailure() throws Exception {
            onlyNewPage(fullArticle("fA001", "第一部", "/poster/a.jpg", "2026/10/12")
                    + fullArticle("fB002", "第二部", "/poster/b.jpg", "2026/11/1"));
            when(movieService.findBySourceId(anyString())).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenAnswer(invocation -> {
                        MovieRequest request = invocation.getArgument(0);
                        if ("fA001".equals(request.sourceId())) {
                            throw new IllegalStateException("db failed");
                        }
                        return saved(2, "fB002", "第二部",
                                BASE + "/poster/b.jpg", true, DATE_B);
                    });

            List<MovieResponse> result = crawler.syncMovies();

            assertEquals(1, result.size());
            assertEquals("fB002", result.get(0).sourceId());
            verify(movieService, org.mockito.Mockito.times(2)).createMovie(any(MovieRequest.class));
        }

        @Test
        @DisplayName("單筆電影違反資料庫限制時，應略過並繼續同步其他電影")
        void partialDataIntegrityFailure() throws Exception {
            onlyNewPage(fullArticle("fA001", "第一部", "/poster/a.jpg", "2026/10/12")
                    + fullArticle("fB002", "第二部", "/poster/b.jpg", "2026/11/1"));
            when(movieService.findBySourceId(anyString())).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenAnswer(invocation -> {
                        MovieRequest request = invocation.getArgument(0);
                        if ("fA001".equals(request.sourceId())) {
                            throw new DataIntegrityViolationException("duplicate");
                        }
                        return saved(2, "fB002", "第二部",
                                BASE + "/poster/b.jpg", true, DATE_B);
                    });

            List<MovieResponse> result = crawler.syncMovies();

            assertEquals(1, result.size());
            assertEquals("fB002", result.get(0).sourceId());
            verify(movieService, org.mockito.Mockito.times(2)).createMovie(any(MovieRequest.class));
        }

        @Test
        @DisplayName("唯一一部電影寫入失敗應回傳 502")
        void allSavesFailed() throws Exception {
            onlyNewPage(fullArticle("fA001", "測試電影", "/poster/a.jpg", "2026/10/12"));
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenThrow(new IllegalStateException("db failed"));

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("電影爬蟲未成功同步任何資料", error.getReason());
        }

        @Test
        @DisplayName("無法取得電影名稱時應略過，且不儲存")
        void missingName() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'></a>");
            pageAt(DETAIL_A, "<html><body><div>沒有電影名稱</div></body></html>");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());

            // 注意：詳情頁若有 HTML title 會被用作電影名稱，所以此測試不設定 title。
            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            verify(movieService, never()).createMovie(any());
        }
    }
}
