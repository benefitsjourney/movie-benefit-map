package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
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
import com.collection.benefitsjourney.enums.County;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CinemaCrawlerService {

    /** HTTP 請求最大等待時間 */
    private static final int REQUEST_TIMEOUT_MS = 10000;

    /** 爬蟲網站影城列表頁面 */
    private static final String ATMOVIES_SHOWTIME_URL = "https://www.atmovies.com.tw/showtime/";

    /** 爬蟲網站經緯度頁面 */
    private static final String ATMOVIES_MAP_URL = "http://ww2.atmovies.com.tw/movie/GMap2018.cfm?tid=";

    /** 經緯度資料正則表達式 */
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("lat:\\s*([0-9.]+)\\s*,\\s*lng:\\s*([0-9.]+)");

    /** 影城 ID 正則表達式 */
    private static final Pattern CINEMA_ID_PATTERN = Pattern.compile("/showtime/([A-Za-z0-9]+)/?");

    /** 經緯度資料 */
    private record Coordinates(
            BigDecimal latitude,
            BigDecimal longitude) {
    }

    private final CinemaService cinemaService;

    public CinemaCrawlerService(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    /** 同步爬取所有影城資料 */
    public List<CinemaResponse> syncCinemas() {
        long startTime = System.nanoTime();

        try {
            Document doc = Jsoup.connect(ATMOVIES_SHOWTIME_URL)
                    .userAgent("Mozilla/5.0")
                    .timeout(REQUEST_TIMEOUT_MS)
                    .get();

            List<String> areaUrls = doc
                    .select("a[href^=/showtime/a]")
                    .stream()
                    .map(a -> a.absUrl("href"))
                    .filter(url -> url.matches(".*/showtime/a\\d{2}/"))
                    .distinct()
                    .toList();

            // 完全抓不到縣市頁面通常代表來源網站結構已改變
            if (areaUrls.isEmpty()) {
                log.error("影城爬蟲找不到任何縣市頁面");

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "找不到影城縣市資料，來源網站結構可能已變更");
            }

            List<CinemaResponse> cinemas = areaUrls
                    .parallelStream()
                    .flatMap(this::getCinemasByAreaUrl)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            // 所有縣市都抓不到影城時，不應視為正常成功
            if (cinemas.isEmpty()) {
                log.error("影城爬蟲未取得任何影城資料");

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "未取得任何影城資料，來源網站可能異常");
            }

            List<CinemaResponse> savedCinemas = cinemas.stream()
                    .map(this::syncCinemaToDatabase)
                    .filter(Objects::nonNull)
                    .toList();

            // 有部分影城寫入失敗時留下紀錄，但不影響其他影城
            if (savedCinemas.size() < cinemas.size()) {
                log.warn(
                        "部分影城同步資料庫失敗，共抓取 {} 間，成功存入 {} 間，失敗 {} 間",
                        cinemas.size(),
                        savedCinemas.size(),
                        cinemas.size() - savedCinemas.size());
            }

            // 全部資料都寫入失敗時，代表同步結果不可視為成功
            if (savedCinemas.isEmpty()) {
                log.error("所有影城資料皆寫入資料庫失敗");

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "影城資料抓取成功，但寫入資料庫失敗");
            }

            double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

            log.info(
                    "影城爬蟲完成，共抓取 {} 間影城，成功存入 {} 間，耗時 {} 秒",
                    cinemas.size(),
                    savedCinemas.size(),
                    String.format("%.2f", seconds));

            return savedCinemas;

        } catch (HttpStatusException e) {
            double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

            log.error(
                    "影城網站回傳 HTTP {}，耗時 {} 秒",
                    e.getStatusCode(),
                    String.format("%.2f", seconds),
                    e);

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "影城來源網站連線異常",
                    e);

        } catch (IOException e) {
            double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

            log.error(
                    "影城來源網站連線失敗，耗時 {} 秒",
                    String.format("%.2f", seconds),
                    e);

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "無法連線至影城來源網站",
                    e);
        }
    }

    /** 抓取指定縣市的影城資料 */
    private Stream<CinemaResponse> getCinemasByAreaUrl(String areaUrl) {
        try {
            Document doc = Jsoup.connect(areaUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(REQUEST_TIMEOUT_MS)
                    .get();

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

    /** 將影城連結轉換為影城資料 */
    private CinemaResponse toCinemaResponse(Element cinemaLink) {
        String cinemaUrl = cinemaLink.absUrl("href");

        if (cinemaUrl.isBlank()) {
            log.warn("略過缺少影城網址的資料");
            return null;
        }

        String sourceId = getCinemaSourceId(cinemaUrl);

        if (sourceId == null || sourceId.isBlank()) {
            log.warn("無法從影城網址解析來源 ID：{}", cinemaUrl);
            return null;
        }

        String cinemaName = cinemaLink.text().trim();

        if (cinemaName.isBlank()) {
            log.warn("略過缺少名稱的影城：{}", sourceId);
            return null;
        }

        Element container = cinemaLink.closest("li");

        String cinemaAddress = getCinemaAddress(container);

        County county = getCounty(cinemaAddress);

        String officialWebsiteUrl = getOfficialWebsiteUrl(container);

        Coordinates coordinates = getCoordinates(sourceId);

        BigDecimal latitude = coordinates != null
                ? coordinates.latitude()
                : null;

        BigDecimal longitude = coordinates != null
                ? coordinates.longitude()
                : null;

        boolean isActive = true;
        boolean isVisible = true;

        return new CinemaResponse(
                null,
                sourceId,
                cinemaName,
                county,
                cinemaAddress,
                latitude,
                longitude,
                officialWebsiteUrl,
                isActive,
                isVisible,
                null,
                null);
    }

    /** 將取得的單筆影城資料同步到資料庫 */
    private CinemaResponse syncCinemaToDatabase(CinemaResponse response) {
        CinemaRequest request = new CinemaRequest(
                response.sourceId(),
                response.cinemaName(),
                response.county(),
                response.cinemaAddress(),
                response.latitude(),
                response.longitude(),
                response.officialWebsiteUrl(),
                response.isActive(),
                response.isVisible());

        try {
            return cinemaService.findBySourceId(response.sourceId())
                    .map(cinema -> cinemaService.updateCinema(
                            cinema.cinemaId(),
                            request))
                    .orElseGet(() -> cinemaService.createCinema(request));

        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "影城資料違反資料庫限制，略過同步：{} ({})",
                    response.cinemaName(),
                    response.sourceId(),
                    e);

            return null;
        }
    }

    /** 取得影城來源 ID */
    private String getCinemaSourceId(String cinemaUrl) {
        if (cinemaUrl == null || cinemaUrl.isBlank()) {
            return null;
        }

        Matcher matcher = CINEMA_ID_PATTERN.matcher(cinemaUrl);

        return matcher.find()
                ? matcher.group(1)
                : null;
    }

    /** 取得影城地址 */
    private String getCinemaAddress(Element container) {
        if (container == null) {
            return null;
        }

        Element addressElement = container.selectFirst("ul li");

        return addressElement != null
                ? addressElement.text().trim()
                : null;
    }

    /** 取得影城官方網站 */
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

        String websiteUrl = websiteLink.absUrl("href");

        return websiteUrl.isBlank()
                ? null
                : websiteUrl;
    }

    /** 取得影城經緯度 */
    private Coordinates getCoordinates(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return null;
        }

        String mapUrl = ATMOVIES_MAP_URL + sourceId;

        try {
            Document doc = Jsoup.connect(mapUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(REQUEST_TIMEOUT_MS)
                    .get();

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

    /** 轉換經緯度格式 */
    private BigDecimal toCoordinate(String value) {
        return new BigDecimal(value)
                .setScale(7, RoundingMode.HALF_UP);
    }

    /** 根據地址取得縣市 */
    private County getCounty(String address) {
        if (address == null || address.isBlank()) {
            return County.UNKNOWN;
        }

        if (address.contains("基隆市")) {
            return County.KEELUNG;
        }

        if (address.contains("台北市")) {
            return County.TAIPEI;
        }

        if (address.contains("新北市")) {
            return County.NEW_TAIPEI;
        }

        if (address.contains("桃園市")) {
            return County.TAOYUAN;
        }

        if (address.contains("新竹市") || address.contains("新竹縣")) {
            return County.HSINCHU;
        }

        if (address.contains("宜蘭縣")) {
            return County.YILAN;
        }

        if (address.contains("苗栗縣")) {
            return County.MIAOLI;
        }

        if (address.contains("台中市")) {
            return County.TAICHUNG;
        }

        if (address.contains("彰化縣")) {
            return County.CHANGHUA;
        }

        if (address.contains("南投縣")) {
            return County.NANTOU;
        }

        if (address.contains("雲林縣")) {
            return County.YUNLIN;
        }

        if (address.contains("嘉義市") || address.contains("嘉義縣")) {
            return County.CHIAYI;
        }

        if (address.contains("台南市")) {
            return County.TAINAN;
        }

        if (address.contains("高雄市")) {
            return County.KAOHSIUNG;
        }

        if (address.contains("屏東縣")) {
            return County.PINGTUNG;
        }

        if (address.contains("花蓮縣")) {
            return County.HUALIEN;
        }

        if (address.contains("台東縣") || address.contains("台東市")) {
            return County.TAITUNG;
        }

        if (address.contains("澎湖縣")) {
            return County.PENGHU;
        }

        if (address.contains("金門縣")) {
            return County.KINMEN;
        }

        if (address.contains("連江縣")) {
            return County.LIENCHIANG;
        }

        return County.UNKNOWN;
    }
}