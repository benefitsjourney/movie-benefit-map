package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
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

import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.enums.County;

/**
 * 不連真實網站。使用 Jsoup.parse 建立假網頁，並 Spy connect() 取代 HTTP 請求。
 * 前提：CinemaCrawlerService 將三處 Jsoup.connect(...).get() 改成 connect(url)，
 * 並新增 package-private 的 Document connect(String url) throws IOException。
 */
@ExtendWith(MockitoExtension.class)
class CinemaCrawlerServiceTest {

    private static final String INDEX = "https://www.atmovies.com.tw/showtime/";
    private static final String AREA_01 = INDEX + "a01/";
    private static final String AREA_02 = INDEX + "a02/";
    private static final String MAP_01 = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=t02a01";
    private static final String MAP_02 = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=t02a02";

    @Mock
    private CinemaService cinemaService;

    @Mock
    private CrawlerReviewsService crawlerReviewsService;

    private CinemaCrawlerService crawler;

    @BeforeEach
    void setUp() {
        crawler = spy(new CinemaCrawlerService(cinemaService, crawlerReviewsService));
    }

    private Document page(String url, String html) {
        return Jsoup.parse(html, url);
    }

    private void index(String html) throws IOException {
        doReturn(page(INDEX, html)).when(crawler).connect(INDEX);
    }

    private void area(String url, String html) throws IOException {
        doReturn(page(url, html)).when(crawler).connect(url);
    }

    private void map(String url, String html) throws IOException {
        doReturn(page(url, html)).when(crawler).connect(url);
    }

    private void oneArea() throws IOException {
        index("<a href='/showtime/a01/'>台北</a>");
    }

    private String cinemaHtml(String id, String name, String address) {
        return """
                <ul>
                  <li>
                    <a href="/showtime/%s/">%s</a>
                    <ul><li>%s</li></ul>
                    <a href="https://www.example.com/">網站</a>
                  </li>
                </ul>
                """.formatted(id, name, address);
    }

    private CinemaResponse saved(int id, String sourceId, String name) {
        return new CinemaResponse(
                id, sourceId, name, County.TAIPEI,
                "台北市信義區松壽路20號", null, null,
                "https://www.example.com/", true, true, null, null);
    }

    @Nested
    @DisplayName("syncCinemas - 正常同步")
    class SyncTests {

        @Test
        @DisplayName("新影城應解析欄位、經緯度並呼叫 createCinema")
        void createCinema() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<script>var p = {lat: 25.035684, lng: 121.566542};</script>");

            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            CinemaResponse saved = saved(100, "t02a01", "信義威秀");
            when(cinemaService.createCinema(any(CinemaRequest.class))).thenReturn(saved);

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(List.of(saved), result);
            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).createCinema(captor.capture());
            CinemaRequest request = captor.getValue();

            assertEquals("t02a01", request.sourceId());
            assertEquals("信義威秀", request.cinemaName());
            assertEquals(County.TAIPEI, request.county());
            assertEquals("台北市信義區松壽路20號", request.cinemaAddress());
            assertEquals("https://www.example.com/", request.officialWebsiteUrl());
            assertEquals(0, new BigDecimal("25.0356840").compareTo(request.latitude()));
            assertEquals(0, new BigDecimal("121.5665420").compareTo(request.longitude()));
            assertEquals(Boolean.TRUE, request.isActive());
            assertEquals(Boolean.TRUE, request.isVisible());
            verify(cinemaService, never()).updateCinema(anyInt(), any());
        }

        @Test
        @DisplayName("已存在且名稱不同時只送審，不改正式資料")
        void queueNameChange() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀新名稱", "台北市信義區松壽路20號"));
            map(MAP_01, "<p>沒有座標</p>");

            CinemaResponse existing = saved(100, "t02a01", "舊名稱");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(existing));

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(List.of(existing), result);
            verify(crawlerReviewsService).submitCrawlerChange(
                    com.collection.benefitsjourney.enums.CinemaResourceType.CINEMA,
                    100,
                    CrawlerReviewsService.CINEMA_NAME,
                    "信義威秀新名稱",
                    "舊名稱");
            verify(cinemaService, never()).updateCinema(anyInt(), any());
            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("空白名稱、網路訂票與重複影城連結應被過濾")
        void skipInvalidAndDuplicates() throws Exception {
            oneArea();
            area(AREA_01, """
                    <ul>
                      <li><a href="/showtime/t02a01/">信義威秀</a>
                          <ul><li>台北市信義區松壽路20號</li></ul></li>
                      <li><a href="/showtime/t02a01/">信義威秀</a>
                          <ul><li>台北市信義區松壽路20號</li></ul></li>
                      <li><a href="/showtime/t02a02/">網路訂票</a></li>
                      <li><a href="/showtime/t02a03/">  </a></li>
                    </ul>
                    """);
            map(MAP_01, "<p>沒有座標</p>");

            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(1, result.size());
            verify(cinemaService).findBySourceId("t02a01");
            verify(cinemaService).createCinema(any(CinemaRequest.class));
            verify(crawler, never()).connect(MAP_02);
        }

        @Test
        @DisplayName("經緯度網站連線失敗時仍可新增影城，座標為 null")
        void mapUnavailable() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            doThrow(new IOException("map unavailable")).when(crawler).connect(MAP_01);

            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            assertEquals(1, crawler.syncCinemas().size());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).createCinema(captor.capture());
            assertNull(captor.getValue().latitude());
            assertNull(captor.getValue().longitude());
        }

        @Test
        @DisplayName("部分影城違反資料庫限制時應略過，其他影城照常成功")
        void partialDatabaseFailure() throws Exception {
            oneArea();
            area(AREA_01,
                    cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號")
                            + cinemaHtml("t02a02", "第二間影城", "台北市中正區測試路1號"));
            map(MAP_01, "<p>無座標</p>");
            map(MAP_02, "<p>無座標</p>");

            when(cinemaService.findBySourceId(anyString())).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenAnswer(invocation -> {
                        CinemaRequest request = invocation.getArgument(0);
                        if ("t02a02".equals(request.sourceId())) {
                            throw new DataIntegrityViolationException("duplicate");
                        }
                        return saved(100, "t02a01", "信義威秀");
                    });

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(1, result.size());
            assertEquals("t02a01", result.get(0).sourceId());
            verify(cinemaService, org.mockito.Mockito.times(2))
                    .createCinema(any(CinemaRequest.class));
        }
    }

    @Nested
    @DisplayName("syncCinemas - 異常處理")
    class ErrorTests {

        @Test
        @DisplayName("首頁找不到縣市連結應回傳 502")
        void noAreaLinks() throws Exception {
            index("<div>來源網站版型更改</div>");

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncCinemas);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("找不到影城縣市資料，來源網站結構可能已變更", error.getReason());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("所有縣市都沒有有效影城應回傳 502")
        void noCinemas() throws Exception {
            oneArea();
            area(AREA_01, "<a href='/showtime/t02a01/'>網路訂票</a>");

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncCinemas);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("未取得任何影城資料，來源網站可能異常", error.getReason());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("首頁 IOException 應轉成 502")
        void indexConnectionFailed() throws Exception {
            doThrow(new IOException("timeout")).when(crawler).connect(INDEX);

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncCinemas);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("無法連線至影城來源網站", error.getReason());
            assertNotNull(error.getCause());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("首頁來源網站回傳 HTTP 錯誤應轉成 502")
        void indexHttpStatusError() throws Exception {
            doThrow(new HttpStatusException("upstream failed", 503, INDEX))
                    .when(crawler).connect(INDEX);

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncCinemas);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("影城來源網站連線異常", error.getReason());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("縣市頁連線失敗且沒有影城時應回傳 502")
        void areaConnectionFailed() throws Exception {
            oneArea();
            doThrow(new IOException("area unavailable")).when(crawler).connect(AREA_01);

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncCinemas);

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
            assertEquals("未取得任何影城資料，來源網站可能異常", error.getReason());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("所有影城都寫入失敗應回傳 500")
        void allDatabaseWritesFailed() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<p>無座標</p>");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, crawler::syncCinemas);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
            assertEquals("影城資料抓取成功，但寫入資料庫失敗", error.getReason());
        }

        @Test
        @DisplayName("首頁存在兩個縣市，其中一個失敗，仍應同步另一個")
        void oneAreaFailureDoesNotStopOthers() throws Exception {
            index("""
                    <a href='/showtime/a01/'>台北</a>
                    <a href='/showtime/a02/'>新北</a>
                    """);
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            doThrow(new IOException("failed")).when(crawler).connect(AREA_02);
            map(MAP_01, "<p>無座標</p>");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(1, result.size());
            assertTrue(result.stream().anyMatch(c -> "t02a01".equals(c.sourceId())));
        }
    }
}
