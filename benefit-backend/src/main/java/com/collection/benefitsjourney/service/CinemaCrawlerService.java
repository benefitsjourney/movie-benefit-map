package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.County;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CinemaCrawlerService {

    /** HTTP 請求最大等待時間 */
    private static final int REQUEST_TIMEOUT_MS = 10_000;
    private static final int HTTP_CONCURRENCY = 5;
    private static final String USER_AGENT = "Mozilla/5.0";

    /** 爬蟲網站影城列表頁面 */
    private static final String ATMOVIES_SHOWTIME_URL = "https://www.atmovies.com.tw/showtime/";

    /** 爬蟲網站經緯度頁面 */
    private static final String ATMOVIES_MAP_URL = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=";

    /** 經緯度資料正則表達式 */
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("lat:\\s*([0-9.]+)\\s*,\\s*lng:\\s*([0-9.]+)");

    /** 影城 ID 正則表達式 */
    private static final Pattern CINEMA_ID_PATTERN = Pattern.compile("/showtime/([A-Za-z0-9]+)/?");

    /** 縣市列表網址格式。 */
    private static final Pattern AREA_URL_PATTERN = Pattern.compile(".*/showtime/a\\d{2}/");

    /** 地址關鍵字與系統縣市的對照，順序即判斷優先順序。 */
    private static final List<CountyMapping> COUNTY_MAPPINGS = List.of(
            new CountyMapping("基隆市", County.KEELUNG),
            new CountyMapping("台北市", County.TAIPEI),
            new CountyMapping("新北市", County.NEW_TAIPEI),
            new CountyMapping("桃園市", County.TAOYUAN),
            new CountyMapping("新竹市", County.HSINCHU),
            new CountyMapping("新竹縣", County.HSINCHU),
            new CountyMapping("宜蘭縣", County.YILAN),
            new CountyMapping("苗栗縣", County.MIAOLI),
            new CountyMapping("台中市", County.TAICHUNG),
            new CountyMapping("彰化縣", County.CHANGHUA),
            new CountyMapping("南投縣", County.NANTOU),
            new CountyMapping("雲林縣", County.YUNLIN),
            new CountyMapping("嘉義市", County.CHIAYI),
            new CountyMapping("嘉義縣", County.CHIAYI),
            new CountyMapping("台南市", County.TAINAN),
            new CountyMapping("高雄市", County.KAOHSIUNG),
            new CountyMapping("屏東縣", County.PINGTUNG),
            new CountyMapping("花蓮縣", County.HUALIEN),
            new CountyMapping("台東縣", County.TAITUNG),
            new CountyMapping("台東市", County.TAITUNG),
            new CountyMapping("澎湖縣", County.PENGHU),
            new CountyMapping("金門縣", County.KINMEN),
            new CountyMapping("連江縣", County.LIENCHIANG));

    /** 來源網站的經緯度原始結果，與影城 DTO 分離。 */
    private record Coordinates(BigDecimal latitude, BigDecimal longitude) {
    }

    private record CountyMapping(String keyword, County county) {
    }

    /** 正式影城資料的查詢與新增入口。 */
    private final CinemaService cinemaService;

    /** 比對既有影城欄位差異並建立審核紀錄。 */
    private final CrawlerReviewsService crawlerReviewsService;

    public CinemaCrawlerService(
            CinemaService cinemaService,
            CrawlerReviewsService crawlerReviewsService) {
        this.cinemaService = cinemaService;
        this.crawlerReviewsService = crawlerReviewsService;
    }

    /** 一般同步：已有完整座標就不重新抓取地圖頁。 */
    public List<CinemaResponse> syncCinemas() {
        return syncCinemas(false);
    }

    /** 完整同步可指定重新檢查所有影城座標。 */
    public List<CinemaResponse> syncCinemas(boolean refreshCoordinates) {
        long startTime = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(HTTP_CONCURRENCY);
        try {
            Document doc = connect(ATMOVIES_SHOWTIME_URL);
            List<String> areaUrls = getAreaUrls(doc);

            if (areaUrls.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "找不到影城縣市資料，來源網站結構可能已變更");
            }

            long areaStart = System.nanoTime();

            // 縣市抓取只分析基本資料，不在這裡逐間請求座標。
            List<CompletableFuture<List<CinemaResponse>>> areaTasks = areaUrls.stream()
                    .map(url -> CompletableFuture.supplyAsync(
                            () -> getCinemasByAreaUrl(url).toList(), executor))
                    .toList();

            List<CinemaResponse> crawled = new ArrayList<>();
            for (var task : areaTasks) {
                crawled.addAll(task.join());
            }

            List<CinemaResponse> cinemas = deduplicateBySourceId(crawled);
            log.info("縣市資料抓取耗時 {} 秒，原始 {} 筆，去重後 {} 間",
                    elapsedSeconds(areaStart), crawled.size(), cinemas.size());

            if (cinemas.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "未取得任何影城資料，來源網站可能異常");
            }

            // 查詢每個sourceId，後續入庫直接重用結果
            long lookupStart = System.nanoTime();
            Map<String, Optional<CinemaResponse>> existing = new LinkedHashMap<>();
            for (CinemaResponse cinema : cinemas) {
                existing.put(cinema.sourceId(), cinemaService.findBySourceId(cinema.sourceId()));
            }

            log.info("既有影城查詢耗時 {} 秒，共 {} 間", elapsedSeconds(lookupStart), existing.size());

            long coordinateStart = System.nanoTime();
            List<CompletableFuture<CinemaResponse>> coordinateTasks = cinemas.stream()
                    .map(cinema -> CompletableFuture.supplyAsync(() -> {
                        CinemaResponse current = existing.get(cinema.sourceId()).orElse(null);
                        if (!refreshCoordinates && current != null
                                && current.latitude() != null && current.longitude() != null) {
                            return cinema; // null 座標表示本次沒抓，不會建立座標變更審核。
                        }
                        Coordinates location = getCoordinates(cinema.sourceId());
                        if (location == null) {
                            return cinema;
                        }
                        return withCoordinates(cinema, location);
                    }, executor))
                    .toList();

            List<CinemaResponse> resolved = coordinateTasks.stream()
                    .map(future -> future.join())
                    .toList();

            long fetchedCoordinates = cinemas.stream().filter(cinema -> {
                CinemaResponse current = existing.get(cinema.sourceId()).orElse(null);
                return refreshCoordinates || current == null
                        || current.latitude() == null || current.longitude() == null;
            }).count();
            log.info("座標階段耗時 {} 秒，需請求 {} 間，略過 {} 間",
                    elapsedSeconds(coordinateStart), fetchedCoordinates, cinemas.size() - fetchedCoordinates);

            long databaseStart = System.nanoTime();
            List<CinemaResponse> saved = resolved.stream()
                    .map(cinema -> syncCinemaToDatabase(cinema, existing.get(cinema.sourceId())))
                    .filter(Objects::nonNull)
                    .toList();

            log.info("資料庫比對及審核耗時 {} 秒", elapsedSeconds(databaseStart));

            if (saved.size() < resolved.size()) {
                log.warn("部分影城同步失敗：抓取 {} 間，成功 {} 間，失敗 {} 間",
                        resolved.size(), saved.size(), resolved.size() - saved.size());
            }

            if (saved.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "影城資料抓取成功，但寫入資料庫失敗");
            }

            log.info("影城爬蟲完成，共處理 {} 間，耗時 {} 秒", saved.size(), elapsedSeconds(startTime));
            return saved;

        } catch (HttpStatusException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "影城來源網站連線異常", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "無法連線至影城來源網站", e);
        } finally {
            executor.shutdown();
        }
    }

    private CinemaResponse withCoordinates(CinemaResponse cinema, Coordinates coordinates) {
        return new CinemaResponse(
                cinema.cinemaId(), cinema.sourceId(), cinema.cinemaName(),
                cinema.county(), cinema.cinemaAddress(),
                coordinates.latitude(), coordinates.longitude(),
                cinema.officialWebsiteUrl(), cinema.isActive(), cinema.isVisible(),
                cinema.createdAt(), cinema.updatedAt());
    }

    /** 統一 HTTP 下載入口，供同 package 的單元測試透過 Spy 模擬 */
    Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MS)
                .get();
    }

    private List<String> getAreaUrls(Document doc) {
        return doc.select("a[href^=/showtime/a]").stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> AREA_URL_PATTERN.matcher(url).matches())
                .distinct()
                .toList();
    }

    private List<CinemaResponse> deduplicateBySourceId(List<CinemaResponse> cinemas) {
        Map<String, CinemaResponse> uniqueCinemas = new LinkedHashMap<>();
        cinemas.forEach(cinema -> uniqueCinemas.putIfAbsent(cinema.sourceId(), cinema));
        return List.copyOf(uniqueCinemas.values());
    }

    /** 抓取單一縣市頁的影城連結；單一縣市失敗不影響其他縣市。 */
    private Stream<CinemaResponse> getCinemasByAreaUrl(String areaUrl) {
        try {
            Document doc = connect(areaUrl);
            // 忽略空白連結與「網路訂票」等非影城項目。
            return doc.select("a[href^=/showtime/t]")
                    .stream()
                    .filter(a -> !a.text().isBlank())
                    .filter(a -> !"網路訂票".equals(a.text().trim()))
                    .map(this::toCinemaResponse)
                    .filter(Objects::nonNull);
        } catch (HttpStatusException e) {
            log.warn(
                    "縣市影城列表回傳 HTTP {}：{}",
                    e.getStatusCode(),
                    areaUrl,
                    e);
            return Stream.empty();
        } catch (IOException e) {
            log.warn(
                    "縣市影城列表抓取失敗：{}",
                    areaUrl,
                    e);
            return Stream.empty();
        }
    }

    /** 將連結轉成影城暫存資料：來源 ID、名稱、地址、縣市、官網與座標 */
    private CinemaResponse toCinemaResponse(Element cinemaLink) {
        String cinemaUrl = cinemaLink.absUrl("href");

        if (cinemaUrl.isBlank()) {
            log.warn("略過缺少影城網址的資料");
            return null;
        }

        String sourceId = getCinemaSourceId(cinemaUrl);
        if (sourceId == null) {
            log.warn("無法從影城網址解析來源 ID：{}", cinemaUrl);
            return null;
        }

        String cinemaName = trimToNull(cinemaLink.text());
        if (cinemaName == null) {
            log.warn("略過缺少名稱的影城：{}", sourceId);
            return null;
        }

        // 從影城所屬列表項目讀取地址與官方網站。
        Element container = cinemaLink.closest("li");
        String cinemaAddress = getCinemaAddress(container);
        County county = getCounty(cinemaAddress);
        String officialWebsiteUrl = getOfficialWebsiteUrl(container);

        return new CinemaResponse(
                null,
                sourceId,
                cinemaName,
                county,
                cinemaAddress,
                null,
                null,
                officialWebsiteUrl,
                true,
                true,
                null,
                null);
    }

    /** 將取得的單筆影城資料同步到資料庫。沒有資料才直接新增，已有資料只送差異去審核 */
    private CinemaResponse syncCinemaToDatabase(CinemaResponse response, Optional<CinemaResponse> existing) {
        try {
            return existing
                    .map(cinema -> {
                        reviewChangedFields(cinema, response);
                        return cinema;
                    })
                    .orElseGet(() -> cinemaService.createCinema(toCinemaRequest(response)));
        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "影城資料違反資料庫限制，略過同步：{} ({})",
                    response.cinemaName(),
                    response.sourceId(),
                    e);
            return null;
        }
    }

    /** 將爬取資料轉為新增影城 API 所需的 Request DTO */
    private CinemaRequest toCinemaRequest(CinemaResponse response) {
        return new CinemaRequest(
                response.sourceId(),
                response.cinemaName(),
                response.county(),
                response.cinemaAddress(),
                response.latitude(),
                response.longitude(),
                response.officialWebsiteUrl(),
                response.isActive(),
                response.isVisible());
    }

    /** 只把實際抓到、且與正式資料不同的欄位送審 */
    private void reviewChangedFields(CinemaResponse current, CinemaResponse crawled) {
        reviewField(current.cinemaId(), CrawlerReviewsService.CINEMA_NAME,
                crawled.cinemaName(), current.cinemaName());

        // 地址不存在時不比對縣市，避免誤判來源未提供地址為實際異動。
        if (crawled.cinemaAddress() != null && !crawled.cinemaAddress().isBlank()) {
            reviewField(current.cinemaId(), CrawlerReviewsService.CINEMA_ADDRESS,
                    crawled.cinemaAddress(), current.cinemaAddress());

            // UNKNOWN 代表無法從地址辨識，不能當成新的有效縣市送審。
            if (crawled.county() != null && crawled.county() != County.UNKNOWN) {
                reviewField(current.cinemaId(), CrawlerReviewsService.COUNTY,
                        crawled.county(), current.county());
            }
        }

        if (crawled.latitude() != null) {
            reviewField(current.cinemaId(), CrawlerReviewsService.LATITUDE,
                    crawled.latitude(), current.latitude());
        }

        if (crawled.longitude() != null) {
            reviewField(current.cinemaId(), CrawlerReviewsService.LONGITUDE,
                    crawled.longitude(), current.longitude());
        }

        if (crawled.officialWebsiteUrl() != null) {
            reviewField(current.cinemaId(), CrawlerReviewsService.OFFICIAL_WEBSITE_URL,
                    crawled.officialWebsiteUrl(), current.officialWebsiteUrl());
        }
    }

    /** 略過未抓到或與正式資料相同的值，其他差異交給審核Service去重複 */
    private void reviewField(Integer entityId, String fieldName, Object crawled, Object current) {
        if (crawled == null || (crawled instanceof String value && value.isBlank())) {
            return;
        }

        String crawlerValue = CrawlerReviewsService.formatValue(crawled);
        String previousValue = CrawlerReviewsService.formatValue(current);

        if (Objects.equals(crawlerValue, previousValue)) {
            return;
        }

        crawlerReviewsService.submitCrawlerChange(
                CinemaResourceType.CINEMA,
                entityId,
                fieldName,
                crawlerValue,
                previousValue);
    }

    /** 從/showtime/tXXXXX/形式的網址擷取影城sourceId */
    private String getCinemaSourceId(String cinemaUrl) {
        if (cinemaUrl == null || cinemaUrl.isBlank()) {
            return null;
        }

        Matcher matcher = CINEMA_ID_PATTERN.matcher(cinemaUrl);
        return matcher.find()
                ? matcher.group(1)
                : null;
    }

    /** 從影城所屬 li 的內部 ul li 解析地址 */
    private String getCinemaAddress(Element container) {
        if (container == null) {
            return null;
        }

        Element addressElement = container.selectFirst("ul li");
        return addressElement == null ? null : trimToNull(addressElement.text());
    }

    /** 尋找文字為「網站」的連結，轉為絕對網址 */
    private String getOfficialWebsiteUrl(Element container) {
        if (container == null) {
            return null;
        }
        Element websiteLink = container.select("a[href]")
                .stream()
                .filter(a -> "網站".equals(a.text().trim()))
                .findFirst()
                .orElse(null);

        if (websiteLink == null) {
            return null;
        }

        return trimToNull(websiteLink.absUrl("href"));
    }

    /** 讀取獨立地圖頁並以正則擷取緯度與經度，失敗則保留空值 */
    private Coordinates getCoordinates(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return null;
        }

        String mapUrl = ATMOVIES_MAP_URL + sourceId;

        try {
            Document doc = connect(mapUrl);
            Matcher matcher = COORDINATE_PATTERN.matcher(doc.html());

            if (!matcher.find()) {
                log.warn(
                        "找不到經緯度：{} / {}",
                        sourceId,
                        mapUrl);
                return null;
            }

            return new Coordinates(
                    toCoordinate(matcher.group(1)),
                    toCoordinate(matcher.group(2)));
        } catch (HttpStatusException e) {
            log.warn(
                    "經緯度頁面回傳 HTTP {}：{}",
                    e.getStatusCode(),
                    sourceId,
                    e);
            return null;
        } catch (IOException e) {
            log.warn(
                    "經緯度頁面抓取失敗：{}",
                    sourceId,
                    e);
            return null;
        } catch (NumberFormatException e) {
            log.warn(
                    "經緯度格式錯誤：{}",
                    sourceId,
                    e);
            return null;
        }
    }

    /** 座標統一取小數點後七位，避免比對時出現不必要的精度差異 */
    private BigDecimal toCoordinate(String value) {
        return new BigDecimal(value)
                .setScale(7, RoundingMode.HALF_UP);
    }

    /** 根據中文地址推斷縣市；無法辨識回傳UNKNOWN */
    private County getCounty(String address) {
        if (address == null || address.isBlank()) {
            return County.UNKNOWN;
        }

        // 將「臺」正規化為「台」，避免地址寫法不同導致UNKNOWN
        String normalizedAddress = address.replace('臺', '台');

        for (CountyMapping mapping : COUNTY_MAPPINGS) {
            if (normalizedAddress.contains(mapping.keyword())) {
                return mapping.county();
            }
        }

        return County.UNKNOWN;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String elapsedSeconds(long startTime) {
        return "%.2f".formatted((System.nanoTime() - startTime) / 1_000_000_000.0);
    }
}
