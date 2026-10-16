package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.County;

/**
 * 不連真實網站。使用 Jsoup.parse 建立假網頁，並 Spy connect() 取代 HTTP 請求。
 * 座標抓取發生在 ExecutorService 工作執行緒，因此不能依賴 mockStatic(Jsoup.class)。
 */
@ExtendWith(MockitoExtension.class)
class CinemaCrawlerServiceTest {

    private static final String INDEX = "https://www.atmovies.com.tw/showtime/";
    private static final String AREA_01 = INDEX + "a01/";
    private static final String AREA_02 = INDEX + "a02/";
    private static final String MAP_01 = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=t02a01";
    private static final String MAP_02 = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=t02a02";
    private static final String MAP_03 = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=t02a03";
    private static final String MAP_04 = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=t02a04";
    private static final String MAP_05 = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=t02a05";

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

    /** 預期不會打到的地圖頁。若真的被呼叫，後續 verify(never) 會失敗。 */
    private void mapNotExpected(String url) throws IOException {
        lenient().doReturn(page(url, "<p>不應被讀取</p>")).when(crawler).connect(url);
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

    private CinemaResponse existingWithCoordinates(
            int id, String sourceId, String name, String address, County county,
            String latitude, String longitude, String website) {
        return new CinemaResponse(
                id, sourceId, name, county, address,
                latitude == null ? null : new BigDecimal(latitude),
                longitude == null ? null : new BigDecimal(longitude),
                website, true, true, null, null);
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
                    CinemaResourceType.CINEMA,
                    100,
                    CrawlerReviewsService.CINEMA_NAME,
                    "信義威秀新名稱",
                    "舊名稱");
            verifyNoMoreInteractions(crawlerReviewsService);
            verify(cinemaService, never()).updateCinema(anyInt(), any());
            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("已有完整座標時，一般同步不重抓地圖，也不送座標審核")
        void skipMapWhenCoordinatesExist() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            mapNotExpected(MAP_01);

            CinemaResponse existing = existingWithCoordinates(
                    100, "t02a01", "信義威秀", "台北市信義區松壽路20號", County.TAIPEI,
                    "25.0000000", "121.0000000", "https://www.example.com/");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(existing));

            assertEquals(List.of(existing), crawler.syncCinemas());
            assertEquals(List.of(existing), crawler.syncCinemas(false));

            verify(crawler, never()).connect(MAP_01);
            verifyNoInteractions(crawlerReviewsService);
            verify(cinemaService, never()).createCinema(any());
            verify(cinemaService, never()).updateCinema(anyInt(), any());
        }

        @Test
        @DisplayName("refreshCoordinates 為 true 時，即使已有座標也應重抓並送審差異")
        void refreshCoordinatesReviewsChanges() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<script>lat: 25.0356840, lng: 121.5665420</script>");

            CinemaResponse existing = existingWithCoordinates(
                    100, "t02a01", "信義威秀", "台北市信義區松壽路20號", County.TAIPEI,
                    "25.0000000", "121.0000000", "https://www.example.com/");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(existing));

            assertEquals(List.of(existing), crawler.syncCinemas(true));

            verify(crawler).connect(MAP_01);
            verify(crawlerReviewsService).submitCrawlerChange(
                    CinemaResourceType.CINEMA, 100, CrawlerReviewsService.LATITUDE,
                    "25.0356840", "25.0000000");
            verify(crawlerReviewsService).submitCrawlerChange(
                    CinemaResourceType.CINEMA, 100, CrawlerReviewsService.LONGITUDE,
                    "121.5665420", "121.0000000");
            verifyNoMoreInteractions(crawlerReviewsService);
            verify(cinemaService, never()).createCinema(any());
            verify(cinemaService, never()).updateCinema(anyInt(), any());
        }

        @Test
        @DisplayName("重抓後座標與正式資料相同時，不應送座標審核")
        void refreshSameCoordinatesNotReviewed() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<script>lat: 25.035684, lng: 121.566542</script>");

            CinemaResponse existing = existingWithCoordinates(
                    100, "t02a01", "信義威秀", "台北市信義區松壽路20號", County.TAIPEI,
                    "25.0356840", "121.5665420", "https://www.example.com/");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(existing));

            assertEquals(List.of(existing), crawler.syncCinemas(true));
            verify(crawler).connect(MAP_01);
            verifyNoInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("既有影城缺少任一座標時，一般同步仍應抓取地圖")
        void fetchMapWhenCoordinateIncomplete() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<script>lat: 25.0356840, lng: 121.5665420</script>");

            CinemaResponse existing = existingWithCoordinates(
                    100, "t02a01", "信義威秀", "台北市信義區松壽路20號", County.TAIPEI,
                    "25.0356840", null, "https://www.example.com/");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(existing));

            assertEquals(List.of(existing), crawler.syncCinemas());

            verify(crawler).connect(MAP_01);
            verify(crawlerReviewsService).submitCrawlerChange(
                    CinemaResourceType.CINEMA, 100, CrawlerReviewsService.LONGITUDE,
                    "121.5665420", null);
            verify(crawlerReviewsService, never()).submitCrawlerChange(
                    any(), any(), eq(CrawlerReviewsService.LATITUDE), any(), any());
            verifyNoMoreInteractions(crawlerReviewsService);
        }

        @Test
        @DisplayName("同一批影城中，已有完整座標者略過地圖，缺座標者仍抓取")
        void skipOnlyCinemasWithCompleteCoordinates() throws Exception {
            oneArea();
            area(AREA_01,
                    cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號")
                            + cinemaHtml("t02a02", "第二間影城", "台北市中正區測試路1號"));
            mapNotExpected(MAP_01);
            map(MAP_02, "<script>lat: 25.0400000, lng: 121.5100000</script>");

            CinemaResponse complete = existingWithCoordinates(
                    100, "t02a01", "信義威秀", "台北市信義區松壽路20號", County.TAIPEI,
                    "25.0000000", "121.0000000", "https://www.example.com/");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(complete));
            when(cinemaService.findBySourceId("t02a02")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(200, "t02a02", "第二間影城"));

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(2, result.size());
            verify(crawler, never()).connect(MAP_01);
            verify(crawler).connect(MAP_02);

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService).createCinema(captor.capture());
            assertEquals("t02a02", captor.getValue().sourceId());
            assertEquals(0, new BigDecimal("25.0400000").compareTo(captor.getValue().latitude()));
            assertEquals(0, new BigDecimal("121.5100000").compareTo(captor.getValue().longitude()));
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
            verify(crawler, never()).connect(MAP_03);
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
            verify(cinemaService, times(2)).createCinema(any(CinemaRequest.class));
        }

        @Test
        @DisplayName("應把臺正規化為台，並依地址關鍵字判斷縣市")
        void resolveCountiesFromAddress() throws Exception {
            oneArea();
            area(AREA_01,
                    cinemaHtml("t02a01", "基隆影城", "基隆市仁愛區愛四路1號")
                            + cinemaHtml("t02a02", "台北影城", "臺北市中正區重慶南路1號")
                            + cinemaHtml("t02a03", "新竹影城", "新竹縣竹北市縣政一路1號")
                            + cinemaHtml("t02a04", "台東影城", "台東市中山路1號")
                            + cinemaHtml("t02a05", "未知影城", "海外市中心1號"));
            map(MAP_01, "<p>無座標</p>");
            map(MAP_02, "<p>無座標</p>");
            map(MAP_03, "<p>無座標</p>");
            map(MAP_04, "<p>無座標</p>");
            map(MAP_05, "<p>無座標</p>");

            when(cinemaService.findBySourceId(anyString())).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenAnswer(invocation -> {
                        CinemaRequest request = invocation.getArgument(0);
                        return saved(100, request.sourceId(), request.cinemaName());
                    });

            assertEquals(5, crawler.syncCinemas().size());

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            verify(cinemaService, times(5)).createCinema(captor.capture());
            Map<String, CinemaRequest> requests = new HashMap<>();
            for (CinemaRequest request : captor.getAllValues()) {
                requests.put(request.sourceId(), request);
            }

            assertEquals(County.KEELUNG, requests.get("t02a01").county());
            assertEquals(County.TAIPEI, requests.get("t02a02").county());
            assertEquals(County.HSINCHU, requests.get("t02a03").county());
            assertEquals(County.TAITUNG, requests.get("t02a04").county());
            assertEquals(County.UNKNOWN, requests.get("t02a05").county());
        }

        @Test
        @DisplayName("不符合縣市頁格式的連結應略過")
        void ignoreInvalidAreaLinks() throws Exception {
            index("""
                    <a href='/showtime/a1/'>格式錯誤</a>
                    <a href='/showtime/about/'>說明</a>
                    <a href='/showtime/a01/'>台北</a>
                    """);
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<p>無座標</p>");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            assertEquals(1, crawler.syncCinemas().size());
            verify(crawler, never()).connect(INDEX + "a1/");
            verify(crawler, never()).connect(INDEX + "about/");
        }

        @Test
        @DisplayName("不同縣市頁重複出現的影城只應新增一次")
        void deduplicateAcrossAreas() throws Exception {
            index("""
                    <a href='/showtime/a01/'>台北</a>
                    <a href='/showtime/a02/'>新北</a>
                    """);
            String html = cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號");
            area(AREA_01, html);
            area(AREA_02, html);
            map(MAP_01, "<p>無座標</p>");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            assertEquals(1, crawler.syncCinemas().size());
            verify(cinemaService).findBySourceId("t02a01");
            verify(cinemaService).createCinema(any(CinemaRequest.class));
        }

        @Test
        @DisplayName("經緯度應四捨五入到小數點後七位")
        void roundCoordinates() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<script>lat: 25.03568415, lng: 121.56654155</script>");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            assertEquals(1, crawler.syncCinemas().size());
            verify(cinemaService).createCinema(captor.capture());
            assertEquals(0, new BigDecimal("25.0356842").compareTo(captor.getValue().latitude()));
            assertEquals(0, new BigDecimal("121.5665416").compareTo(captor.getValue().longitude()));
        }

        @Test
        @DisplayName("經緯度格式錯誤時仍可新增影城，座標為 null")
        void invalidCoordinates() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            map(MAP_01, "<script>lat: 1.2.3, lng: 121.5</script>");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            assertEquals(1, crawler.syncCinemas().size());
            verify(cinemaService).createCinema(captor.capture());
            assertNull(captor.getValue().latitude());
            assertNull(captor.getValue().longitude());
        }

        @Test
        @DisplayName("經緯度頁回傳 HTTP 錯誤時仍可新增影城，座標為 null")
        void mapHttpStatusFailed() throws Exception {
            oneArea();
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            doThrow(new HttpStatusException("map failed", 404, MAP_01))
                    .when(crawler).connect(MAP_01);
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            ArgumentCaptor<CinemaRequest> captor = ArgumentCaptor.forClass(CinemaRequest.class);
            assertEquals(1, crawler.syncCinemas().size());
            verify(cinemaService).createCinema(captor.capture());
            assertNull(captor.getValue().latitude());
            assertNull(captor.getValue().longitude());
        }

        @Test
        @DisplayName("一般同步只送審地址、縣市與官網；已有座標時不送座標審核，未知縣市也不送審")
        void queueChangedAddressCountyAndWebsite() throws Exception {
            oneArea();
            area(AREA_01, """
                    <ul>
                      <li>
                        <a href="/showtime/t02a01/">信義威秀</a>
                        <ul><li>臺北市信義區松壽路20號</li></ul>
                        <a href="https://www.example.com/">網站</a>
                      </li>
                      <li>
                        <a href="/showtime/t02a02/">未知影城</a>
                        <ul><li>海外市中心1號</li></ul>
                        <a href="https://www.other.example/">網站</a>
                      </li>
                    </ul>
                    """);
            mapNotExpected(MAP_01);
            map(MAP_02, "<p>無座標</p>");

            CinemaResponse taipei = existingWithCoordinates(
                    100, "t02a01", "信義威秀", "新北市板橋區文化路1號", County.NEW_TAIPEI,
                    "25.0000000", "121.0000000", "https://www.example.com/");
            CinemaResponse unknown = existingWithCoordinates(
                    200, "t02a02", "未知影城", "台北市中正區重慶南路1號", County.TAIPEI,
                    null, null, "https://www.old.example/");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(taipei));
            when(cinemaService.findBySourceId("t02a02")).thenReturn(Optional.of(unknown));

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(2, result.size());
            verify(crawler, never()).connect(MAP_01);
            verify(crawler).connect(MAP_02);
            verify(crawlerReviewsService).submitCrawlerChange(
                    CinemaResourceType.CINEMA,
                    100, CrawlerReviewsService.CINEMA_ADDRESS,
                    "臺北市信義區松壽路20號", "新北市板橋區文化路1號");
            verify(crawlerReviewsService).submitCrawlerChange(
                    CinemaResourceType.CINEMA,
                    100, CrawlerReviewsService.COUNTY,
                    County.TAIPEI.name(), County.NEW_TAIPEI.name());
            verify(crawlerReviewsService).submitCrawlerChange(
                    CinemaResourceType.CINEMA,
                    200, CrawlerReviewsService.CINEMA_ADDRESS,
                    "海外市中心1號", "台北市中正區重慶南路1號");
            verify(crawlerReviewsService).submitCrawlerChange(
                    CinemaResourceType.CINEMA,
                    200, CrawlerReviewsService.OFFICIAL_WEBSITE_URL,
                    "https://www.other.example/", "https://www.old.example/");
            verify(crawlerReviewsService, never()).submitCrawlerChange(
                    any(), eq(100), eq(CrawlerReviewsService.LATITUDE), any(), any());
            verify(crawlerReviewsService, never()).submitCrawlerChange(
                    any(), eq(100), eq(CrawlerReviewsService.LONGITUDE), any(), any());
            verify(crawlerReviewsService, never()).submitCrawlerChange(
                    any(), eq(200), eq(CrawlerReviewsService.COUNTY), any(), any());
            verify(cinemaService, never()).updateCinema(anyInt(), any());
            verify(cinemaService, never()).createCinema(any());
        }

        @Test
        @DisplayName("來源沒有地址時，不應把縣市送審")
        void blankAddressDoesNotQueueCounty() throws Exception {
            oneArea();
            area(AREA_01, """
                    <ul>
                      <li><a href="/showtime/t02a01/">信義威秀</a></li>
                    </ul>
                    """);
            map(MAP_01, "<p>無座標</p>");
            CinemaResponse existing = saved(100, "t02a01", "信義威秀");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.of(existing));

            assertEquals(List.of(existing), crawler.syncCinemas());
            verifyNoInteractions(crawlerReviewsService);
            verify(cinemaService, never()).updateCinema(anyInt(), any());
            verify(cinemaService, never()).createCinema(any());
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

        @Test
        @DisplayName("縣市頁回傳 HTTP 錯誤時，仍應同步其他縣市")
        void areaHttpStatusDoesNotStopOthers() throws Exception {
            index("""
                    <a href='/showtime/a01/'>台北</a>
                    <a href='/showtime/a02/'>新北</a>
                    """);
            area(AREA_01, cinemaHtml("t02a01", "信義威秀", "台北市信義區松壽路20號"));
            doThrow(new HttpStatusException("area failed", 503, AREA_02))
                    .when(crawler).connect(AREA_02);
            map(MAP_01, "<p>無座標</p>");
            when(cinemaService.findBySourceId("t02a01")).thenReturn(Optional.empty());
            when(cinemaService.createCinema(any(CinemaRequest.class)))
                    .thenReturn(saved(100, "t02a01", "信義威秀"));

            List<CinemaResponse> result = crawler.syncCinemas();

            assertEquals(1, result.size());
            assertEquals("t02a01", result.get(0).sourceId());
        }
    }
}
