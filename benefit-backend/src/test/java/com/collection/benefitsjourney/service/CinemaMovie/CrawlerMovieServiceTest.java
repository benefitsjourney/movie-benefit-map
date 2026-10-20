package com.collection.benefitsjourney.service.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerSyncResult;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;

/**
 * 透過 Spy connect() 模擬網站，Mockito Mock 模擬資料庫服務。
 * 前提：CrawlerMovieService 的 connect(String url) 維持 package-private。
 * 不能依賴 mockStatic(Jsoup.class)：電影列表是在 ExecutorService 工作執行緒下載。
 * 只有 movie 表已有的 sourceId 才會再呼叫 findBySourceId；詳細頁以完成順序處理。
 */
@ExtendWith(MockitoExtension.class)
class CrawlerMovieServiceTest {

    private static final String BASE = "https://www.atmovies.com.tw";
    private static final String NEW = BASE + "/movie/new/";
    private static final String NOW = BASE + "/movie/now/0/";
    private static final String NOW2 = BASE + "/movie/now2/0/";
    private static final String NEXT = BASE + "/movie/next/0/";
    private static final String NEXT2 = BASE + "/movie/next2/";
    private static final String DETAIL_A = BASE + "/movie/fA001/";
    private static final String DETAIL_B = BASE + "/movie/fB002/";
    private static final String WEEK = BASE + "/movie/next/w3/";

    private static final LocalDateTime DATE_A = LocalDateTime.of(2026, 10, 12, 0, 0);
    private static final LocalDateTime DATE_B = LocalDateTime.of(2026, 11, 1, 0, 0);

    @Mock
    private MovieService movieService;

    @Mock
    private CinemaMovieReviewsService cinemaMovieReviewsService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CrawlerMovieService crawlerMovieService;
    private Logger logger;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        crawlerMovieService = spy(new CrawlerMovieService(movieService, cinemaMovieReviewsService, jdbcTemplate));
        logger = (Logger) LoggerFactory.getLogger(CrawlerMovieService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        lenient().when(cinemaMovieReviewsService.submitCrawlerChange(any(), any(), any(), any(), any()))
                .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    private void assertSummary(
            CinemaMovieCrawlerSyncResult<?> result, int fetched, int created, int skipped, int updated, int reviewed) {
        assertEquals(fetched, result.fetchedCount());
        assertEquals(created, result.createdCount());
        assertEquals(skipped, result.skippedCount());
        assertEquals(updated, result.updatedCount());
        assertEquals(reviewed, result.reviewCount());
        assertTrue(result.elapsedSeconds() >= 0);
        assertEquals(
                "本次抓到 %d 筆、新增 %d 筆、略過 %d 筆、更新 %d 筆、納入審核 %d 筆，耗時 %d 秒。"
                        .formatted(fetched, created, skipped, updated, reviewed, result.elapsedSeconds()),
                result.message());
    }

    private void assertOnlyResultLog(CinemaMovieCrawlerSyncResult<?> result) {
        var infos = logs.list.stream().filter(event -> event.getLevel() == Level.INFO).toList();
        assertEquals(1, infos.size());
        assertEquals("電影同步完成：" + result.message(), infos.get(0).getFormattedMessage());
    }

    private Document page(String url, String html) {
        return Jsoup.parse(html, url);
    }

    private void pageAt(String url, String html) throws IOException {
        doReturn(page(url, html)).when(crawlerMovieService).connect(url);
    }

    private void pages(String newHtml, String nowHtml, String now2Html, String nextHtml, String next2Html)
            throws IOException {
        pageAt(NEW, newHtml);
        pageAt(NOW, nowHtml);
        pageAt(NOW2, now2Html);
        pageAt(NEXT, nextHtml);
        pageAt(NEXT2, next2Html);
    }

    private void onlyNewPage(String newHtml) throws IOException {
        pages(newHtml, "<html></html>", "<html></html>", "<html></html>", "<html></html>");
    }

    private String titled(String title, String body) {
        return """
                <html><head><title>%s</title></head><body>%s</body></html>
                """.formatted(title, body);
    }

    private String detail(String title, String date) {
        String body = date == null ? "" : "<p>上映日期：" + date + "</p>";
        return titled(title, body);
    }

    private String poster(String sourceId) {
        return BASE + "/photo101/" + sourceId + "/pl_" + sourceId + ".jpg";
    }

    private MovieResponse saved(int id, String sourceId, String name,
            String imageUrl, boolean visible, LocalDateTime date) {
        return new MovieResponse(id, sourceId, BASE + "/movie/" + sourceId + "/",
                name, imageUrl, visible, date, null, null);
    }

    private void givenStored(int id, String sourceId, String name) {
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
            when(rs.getInt("movie_id")).thenReturn(id);
            when(rs.getString("source_id")).thenReturn(sourceId);
            when(rs.getString("movie_name")).thenReturn(name);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class));
    }

    /** 來源 ID 已在 movie 表時，同步才會載入完整電影做差異審核。 */
    private void givenExisting(MovieResponse existing) {
        givenStored(existing.movieId(), existing.sourceId(), existing.movieName());
        when(movieService.findBySourceId(existing.sourceId())).thenReturn(Optional.of(existing));
    }

    private MovieRequest captureCreated() {
        ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
        verify(movieService).createMovie(captor.capture());
        return captor.getValue();
    }

    private String movieName(ArgumentCaptor<MovieRequest> captor, String sourceId) {
        return captor.getAllValues().stream()
                .filter(request -> sourceId.equals(request.sourceId()))
                .map(request -> request.movieName())
                .findFirst()
                .orElseThrow();
    }

    @Nested
    @DisplayName("syncMovies - 詳細頁解析與新增")
    class CreateTests {

        @Test
        @DisplayName("只從詳細頁頁籤取名，海報固定使用 photo101，列表文字不採用")
        void createFromDetailTitle() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表上的名稱不採用</a>");
            pageAt(DETAIL_A, detail("測試電影 English Title @movies〖開眼電影網〗", "2026/10/12"));
            MovieResponse created = saved(1, "fA001", "測試電影", poster("fA001"), true, DATE_A);
            when(movieService.createMovie(any(MovieRequest.class))).thenReturn(created);

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();
            assertEquals(List.of(created), result.items());
            assertSummary(result, 1, 1, 0, 0, 0);
            assertOnlyResultLog(result);

            MovieRequest request = captureCreated();
            assertEquals("fA001", request.sourceId());
            assertEquals(DETAIL_A, request.sourceUrl());
            assertEquals("測試電影", request.movieName());
            assertEquals(poster("fA001"), request.imageUrl());
            assertEquals(Boolean.TRUE, request.isVisible());
            assertEquals(DATE_A, request.releaseDate());
            verify(movieService, never()).findBySourceId(anyString());
            verify(crawlerMovieService).connect(DETAIL_A);
            verify(crawlerMovieService).connect(NEXT2);
        }

        @Test
        @DisplayName("頁籤優先於標題錨點；英文譯名後的劇情冒號應整段移除")
        void preferPageTitleAndStripDescribedTranslation() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, """
                    <html><head><title>正式名稱 English Title：這是劇情介紹 @movies</title></head>
                    <body>
                      <div class="filmTitle"><a href="/movie/fA001/">不該採用的錨點</a>父層介紹</div>
                      <p>上映日期：10/12/2026</p>
                    </body></html>
                    """);
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "正式名稱", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            MovieRequest request = captureCreated();
            assertEquals("正式名稱", request.movieName());
            assertEquals(DATE_A, request.releaseDate());
        }

        @Test
        @DisplayName("頁籤沒有網站後綴時，才用標題錨點的自身文字，不讀父層介紹")
        void fallbackToAnchorOwnText() throws Exception {
            onlyNewPage("<a href='/film/fA001/?from=list#top'>外層</a>");
            pageAt(DETAIL_A, """
                    <html><head>
                      <title>沒有網站後綴</title>
                      <meta property="og:title" content="不該用的社群標題〖開眼電影網〗">
                    </head><body>
                      <div class="filmTitle"><a href="/movie/fA001/">錨點片名 @movies</a>父層劇情不採用</div>
                      <p>上映日期：2026/10/12</p>
                    </body></html>
                    """);
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "錨點片名", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals("錨點片名", captureCreated().movieName());
            verify(crawlerMovieService).connect(DETAIL_A);
        }

        @Test
        @DisplayName("錨點無效時改用 og:title，並保留中文副標")
        void fallbackToOpenGraphAndKeepSubtitle() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, """
                    <html><head>
                      <title>電影</title>
                      <meta property="og:title" content="片名：特別篇 @movies">
                    </head><body><p>上映日期：2026/10/12</p></body></html>
                    """);
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "片名：特別篇", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals("片名：特別篇", captureCreated().movieName());
        }

        @Test
        @DisplayName("中文名稱可以數字或算式結尾，只去掉後面的英文譯名與介紹後綴")
        void keepNumericEndingAndDropIntroSuffix() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("2+2=5 的電影 Two Plus Two｜劇情簡介 @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "2+2=5 的電影", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals("2+2=5 的電影", captureCreated().movieName());
        }

        @Test
        @DisplayName("純英文片名可以新增，長度超過 64 字也保留")
        void acceptLongEnglishTitle() throws Exception {
            String title = "This Is A Very Long English Movie Title That Exceeds Sixty Four Characters Easily";
            assertTrue(title.length() > 64);
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail(title + " @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", title, poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals(title, captureCreated().movieName());
        }

        @Test
        @DisplayName("Live Viewing 英文副名含特殊符號時，只保留前面的中文片名")
        void stripLiveViewingTranslation() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("櫻坂46演唱會 Live Viewing “Blooming★Parade” @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "櫻坂46演唱會", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals("櫻坂46演唱會", captureCreated().movieName());
        }

        @Test
        @DisplayName("英文片名的重複前綴只留較完整的後半段")
        void collapseRepeatedEnglishTitle() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>甲</a><a href='/movie/fB002/'>乙</a>");
            pageAt(DETAIL_A, detail(
                    "SB19 Wakas at Simula SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas @movies",
                    "2026/10/12"));
            pageAt(DETAIL_B, detail(
                    "Perfume“Cold Sleep” Perfume“Cold Sleep”-25 years Document @movies",
                    "2026/11/1"));
            when(movieService.createMovie(any(MovieRequest.class))).thenAnswer(invocation -> {
                MovieRequest request = invocation.getArgument(0);
                return saved(request.sourceId().equals("fA001") ? 1 : 2,
                        request.sourceId(), request.movieName(), request.imageUrl(), true, request.releaseDate());
            });

            assertEquals(2, crawlerMovieService.syncMovies().items().size());
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService, org.mockito.Mockito.times(2)).createMovie(captor.capture());
            assertEquals("SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas",
                    movieName(captor, "fA001"));
            assertEquals("Perfume“Cold Sleep”-25 years Document",
                    movieName(captor, "fB002"));
        }

        @Test
        @DisplayName("英文片名只是前綴相似、後面沒有分隔時不拆開")
        void keepEnglishTitleWithoutSeparatedRepeat() throws Exception {
            String title = "The Matrix The MatrixReloaded";
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail(title + " @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", title, poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals(title, captureCreated().movieName());
        }

        @Test
        @DisplayName("無法解析的上映日期應略過，電影仍可新增")
        void unparseableReleaseDate() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "13/40/2026"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影", poster("fA001"), true, null));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals("測試電影", captureCreated().movieName());
            assertNull(captureCreated().releaseDate());
        }

        @Test
        @DisplayName("即將上映的週次頁也要抓，其他列表上的週次連結則忽略")
        void includeWeeklyNextPagesOnly() throws Exception {
            pages("<a href='/movie/next/w9/'>不該跟這頁</a>", "", "",
                    "<a href='/movie/next/w3/'>第三週</a>", "");
            pageAt(WEEK, "<a href='/movie/fA001/'>週次列表</a>");
            pageAt(DETAIL_A, detail("週次電影 @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "週次電影", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            assertEquals("週次電影", captureCreated().movieName());
            verify(crawlerMovieService).connect(WEEK);
            verify(crawlerMovieService, never()).connect(BASE + "/movie/next/w9/");
        }

        @Test
        @DisplayName("同一部電影跨列表重複出現，只應新增一次")
        void deduplicateAcrossPages() throws Exception {
            pages("<a href='/movie/fA001/'>甲</a>",
                    "<a href='/movie/fA001/'>乙</a>",
                    "<a href='/film/fA001/'>丙</a>", "", "");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            verify(movieService).createMovie(any(MovieRequest.class));
            verify(crawlerMovieService).connect(DETAIL_A);
        }

        @Test
        @DisplayName("非法電影網址應被忽略")
        void ignoreUnrelatedLinks() throws Exception {
            onlyNewPage("""
                    <a href='/showtime/t02a01/'>影城</a>
                    <a href='/movie/new/'>本週新片</a>
                    <a href='https://example.com/movie/fA001/'>外站電影</a>
                    """);

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawlerMovieService::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("電影網站沒有回傳可用列表", error.getReason());
            verifyNoInteractions(movieService);
            assertTrue(logs.list.stream().noneMatch(event -> event.getLevel() == Level.INFO));
        }
    }

    @Nested
    @DisplayName("syncMovies - 已存在與同名電影")
    class ExistingTests {

        @Test
        @DisplayName("內容相同時回傳原資料，不送審")
        void unchangedMovie() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            MovieResponse existing = saved(1, "fA001", "測試電影", poster("fA001"), false, DATE_A);
            givenExisting(existing);

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();

            assertEquals(1, result.items().size());
            assertSame(existing, result.items().get(0));
            assertSummary(result, 1, 0, 0, 0, 0);
            verifyNoInteractions(cinemaMovieReviewsService);
            verify(movieService, never()).createMovie(any());
            verify(movieService, never()).updateMovie(any(), any());
        }

        @Test
        @DisplayName("既有電影沒有來源網址時直接補上，不送審")
        void fillMissingSourceUrl() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            MovieResponse existing = new MovieResponse(
                    1, "fA001", null, "測試電影", poster("fA001"), false, DATE_A, null, null);
            givenExisting(existing);
            MovieResponse filled = saved(1, "fA001", "測試電影", poster("fA001"), false, DATE_A);
            when(movieService.updateMovie(eq(1), any(MovieRequest.class))).thenReturn(filled);

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();

            assertSame(filled, result.items().get(0));
            assertSummary(result, 1, 0, 0, 1, 0);
            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).updateMovie(eq(1), captor.capture());
            assertEquals(DETAIL_A, captor.getValue().sourceUrl());
            assertNull(captor.getValue().movieName());
            verifyNoInteractions(cinemaMovieReviewsService);
            verify(movieService, never()).createMovie(any());
        }

        @Test
        @DisplayName("來源網址不同時只送審，不改正式資料")
        void queueSourceUrlChange() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            MovieResponse existing = new MovieResponse(
                    1, "fA001", "https://old.example/movie", "測試電影", poster("fA001"),
                    false, DATE_A, null, null);
            givenExisting(existing);

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();

            assertEquals(List.of(existing), result.items());
            assertSummary(result, 1, 0, 0, 1, 1);
            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 1, CinemaMovieReviewsService.SOURCE_URL,
                    DETAIL_A, "https://old.example/movie");
            verify(movieService, never()).updateMovie(any(), any());
            verify(movieService, never()).createMovie(any());
        }

        @Test
        @DisplayName("名稱、海報與上映日期不同時只送審變更欄位")
        void queueChangedFields() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("更新後名稱 @movies", "2026/11/1"));
            MovieResponse existing = saved(1, "fA001", "舊名稱", BASE + "/poster/old.jpg", false, DATE_A);
            givenExisting(existing);

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();
            assertEquals(List.of(existing), result.items());
            assertSummary(result, 1, 0, 0, 1, 3);

            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 1, CinemaMovieReviewsService.MOVIE_NAME, "更新後名稱", "舊名稱");
            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 1, CinemaMovieReviewsService.IMAGE_URL,
                    poster("fA001"), BASE + "/poster/old.jpg");
            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 1, CinemaMovieReviewsService.RELEASE_DATE,
                    "2026-11-01 00:00:00", "2026-10-12 00:00:00");
            verify(movieService, never()).createMovie(any());
        }

        @Test
        @DisplayName("詳細頁沒有上映日期時，不拿空值覆蓋正式日期")
        void skipNullReleaseDateReview() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", null));
            MovieResponse existing = saved(1, "fA001", "測試電影", poster("fA001"), false, DATE_A);
            givenExisting(existing);

            assertEquals(List.of(existing), crawlerMovieService.syncMovies().items());

            verify(cinemaMovieReviewsService, never()).submitCrawlerChange(
                    any(), any(), eq(CinemaMovieReviewsService.RELEASE_DATE), any(), any());
            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("網站略去版本註記時，不把真人版或修復版改成一般片名")
        void ignoreEditionSuffix() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            List<String> storedNames = List.of(
                    "測試電影（真人版）",
                    "測試電影(數位修復版)",
                    "測試電影《數字修復版》",
                    "測試電影〈修復版〉",
                    "測試電影【2K修復版】",
                    "測試電影（4K修復版）",
                    "測試 電影（真人版）");

            for (String storedName : storedNames) {
                MovieResponse existing = saved(1, "fA001", storedName, poster("fA001"), false, DATE_A);
                givenExisting(existing);
                assertEquals(List.of(existing), crawlerMovieService.syncMovies().items(), storedName);
            }

            verifyNoInteractions(cinemaMovieReviewsService);
        }

        @Test
        @DisplayName("不是版本註記的括號副標仍要送審")
        void reviewNonEditionSuffix() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            MovieResponse existing = saved(1, "fA001", "測試電影（導演版）", poster("fA001"), false, DATE_A);
            givenExisting(existing);

            assertEquals(List.of(existing), crawlerMovieService.syncMovies().items());

            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 1, CinemaMovieReviewsService.MOVIE_NAME,
                    "測試電影", "測試電影（導演版）");
        }

        @Test
        @DisplayName("正規化後同名但來源不同，應送審來源衝突且不新增")
        void queueDuplicateName() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("test 測試電影 @movies", "2026/10/12"));
            givenStored(8, "fOLD", "Test　測試電影！");

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();
            assertEquals(List.of(), result.items());
            assertSummary(result, 1, 0, 0, 0, 1);

            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 8, CinemaMovieReviewsService.SOURCE_ID, "fA001", "fOLD");
            verify(movieService, never()).findBySourceId(anyString());
            verify(movieService, never()).createMovie(any());
        }

        @Test
        @DisplayName("同一次同步先新增的電影，後面的同名片不再新增")
        void duplicateWithinSameRun() throws Exception {
            onlyNewPage("""
                    <a href='/movie/fA001/'>甲</a>
                    <a href='/movie/fB002/'>乙</a>
                    """);
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            pageAt(DETAIL_B, detail("測試電影 English @movies", "2026/11/1"));
            when(movieService.createMovie(any(MovieRequest.class))).thenAnswer(invocation -> {
                MovieRequest request = invocation.getArgument(0);
                return saved(1, request.sourceId(), request.movieName(), request.imageUrl(), true,
                        request.releaseDate());
            });

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();

            assertEquals(1, result.items().size());
            String createdSource = result.items().get(0).sourceId();
            String duplicateSource = "fA001".equals(createdSource) ? "fB002" : "fA001";
            assertSummary(result, 2, 1, 0, 0, 1);
            verify(movieService).createMovie(any(MovieRequest.class));
            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 1, CinemaMovieReviewsService.SOURCE_ID,
                    duplicateSource, createdSource);
        }

        @Test
        @DisplayName("已儲存的重複英文前綴，會送審成較完整的後半段")
        void reviewCollapsedEnglishTitle() throws Exception {
            String collapsed = "SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas";
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail(
                    "SB19 Wakas at Simula " + collapsed + " @movies", "2026/10/12"));
            MovieResponse existing = saved(1, "fA001",
                    "SB19 Wakas at Simula " + collapsed, poster("fA001"), false, DATE_A);
            givenExisting(existing);

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();
            assertEquals(List.of(existing), result.items());
            assertSummary(result, 1, 0, 0, 1, 1);
            verify(cinemaMovieReviewsService).submitCrawlerChange(
                    CinemaResourceType.MOVIE, 1, CinemaMovieReviewsService.MOVIE_NAME,
                    collapsed, existing.movieName());
        }

        @Test
        @DisplayName("名稱正規化後對到同一來源時，只略過不送審")
        void sameSourceDuplicateIsSkipped() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            givenStored(8, "fA001", "測試電影");
            when(movieService.findBySourceId("fA001")).thenReturn(Optional.empty());

            assertEquals(List.of(), crawlerMovieService.syncMovies().items());

            verify(movieService).findBySourceId("fA001");
            verifyNoInteractions(cinemaMovieReviewsService);
            verify(movieService, never()).createMovie(any());
        }
    }

    @Nested
    @DisplayName("syncMovies - 異常與部分成功")
    class ErrorTests {

        @Test
        @DisplayName("列表沒有電影連結時應回傳 502")
        void emptyLists() throws Exception {
            onlyNewPage("<html><body>目前沒有電影</body></html>");

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawlerMovieService::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("電影網站沒有回傳可用列表", error.getReason());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("五個電影列表全都連線失敗應回傳 502")
        void allListsFailed() throws Exception {
            for (String url : List.of(NEW, NOW, NOW2, NEXT, NEXT2)) {
                doThrow(new IOException("timeout")).when(crawlerMovieService).connect(url);
            }

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawlerMovieService::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("電影網站沒有回傳可用列表", error.getReason());
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("部分列表失敗時仍能同步其他列表的電影")
        void partialListFailure() throws Exception {
            pageAt(NEW, "<a href='/movie/fA001/'>列表</a>");
            doThrow(new IOException("second list failed")).when(crawlerMovieService).connect(NOW);
            pageAt(NOW2, "");
            pageAt(NEXT, "");
            pageAt(NEXT2, "");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenReturn(saved(1, "fA001", "測試電影", poster("fA001"), true, DATE_A));

            assertEquals(1, crawlerMovieService.syncMovies().items().size());
            verify(movieService).createMovie(any(MovieRequest.class));
        }

        @Test
        @DisplayName("詳細頁連線或 HTTP 錯誤時應略過該片")
        void detailFetchFailed() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a><a href='/movie/fB002/'>列表</a>");
            doThrow(new IOException("detail failed")).when(crawlerMovieService).connect(DETAIL_A);
            doThrow(new IOException("HTTP 404：" + DETAIL_B)).when(crawlerMovieService).connect(DETAIL_B);

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawlerMovieService::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("沒有任何電影同步成功", error.getReason());
            verify(movieService, never()).createMovie(any());
        }

        @Test
        @DisplayName("頁籤只剩通用標題時應略過")
        void genericPageTitleIsSkipped() throws Exception {
            for (String title : List.of("電影 @movies", "開眼電影網〖開眼電影網〗")) {
                onlyNewPage("<a href='/movie/fA001/'>列表</a>");
                pageAt(DETAIL_A, detail(title, "2026/10/12"));

                ResponseStatusException error = assertThrows(
                        ResponseStatusException.class, crawlerMovieService::syncMovies, title);

                assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode(), title);
                assertEquals("沒有任何電影同步成功", error.getReason(), title);
            }

            verify(movieService, never()).createMovie(any());
            assertTrue(logs.list.stream().anyMatch(event -> event.getLevel() == Level.WARN
                    && event.getFormattedMessage().contains("電影詳細頁缺少有效片名")));
        }

        @Test
        @DisplayName("單筆電影儲存失敗時，應略過並繼續同步其他電影")
        void partialSaveFailure() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>甲</a><a href='/movie/fB002/'>乙</a>");
            pageAt(DETAIL_A, detail("第一部 @movies", "2026/10/12"));
            pageAt(DETAIL_B, detail("第二部 @movies", "2026/11/1"));
            when(movieService.createMovie(any(MovieRequest.class))).thenAnswer(invocation -> {
                MovieRequest request = invocation.getArgument(0);
                if ("fA001".equals(request.sourceId())) {
                    throw new DataIntegrityViolationException("duplicate");
                }
                return saved(2, "fB002", "第二部", poster("fB002"), true, DATE_B);
            });

            CinemaMovieCrawlerSyncResult<MovieResponse> result = crawlerMovieService.syncMovies();

            assertEquals(1, result.items().size());
            assertEquals("fB002", result.items().get(0).sourceId());
            assertSummary(result, 2, 1, 0, 0, 0);
            verify(movieService, org.mockito.Mockito.times(2)).createMovie(any(MovieRequest.class));
        }

        @Test
        @DisplayName("唯一一部電影寫入失敗應回傳 502")
        void allSavesFailed() throws Exception {
            onlyNewPage("<a href='/movie/fA001/'>列表</a>");
            pageAt(DETAIL_A, detail("測試電影 @movies", "2026/10/12"));
            when(movieService.createMovie(any(MovieRequest.class)))
                    .thenThrow(new IllegalStateException("db failed"));

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawlerMovieService::syncMovies);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("沒有任何電影同步成功", error.getReason());
        }
    }
}
