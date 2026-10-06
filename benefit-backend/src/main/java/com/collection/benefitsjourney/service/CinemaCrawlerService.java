package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

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
            List<CinemaResponse> cinemas = Jsoup.connect(ATMOVIES_SHOWTIME_URL)
                    .userAgent("Mozilla/5.0")
                    .timeout(REQUEST_TIMEOUT_MS)
                    .get()
                    .select("a[href^=/showtime/a]")
                    .stream()
                    .map(a -> a.absUrl("href"))
                    .filter(url -> url.matches(".*/showtime/a\\d{2}/"))
                    .distinct()
                    .parallel()
                    .flatMap(this::getCinemasByAreaUrl)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<CinemaResponse> savedCinemas = cinemas.stream()
                    .map(this::syncCinemasToDatabase)
                    .toList();

            double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

            log.info("影城爬蟲完成，共抓取 {} 間影城，成功存入 {} 間，耗時 {} 秒",
                    cinemas.size(),
                    savedCinemas.size(),
                    String.format("%.2f", seconds));

            return cinemas;

        } catch (Exception e) {
            double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

            log.error("影城爬蟲失敗，耗時 {} 秒",
                    String.format("%.2f", seconds), e);

            throw new RuntimeException("抓取影城資料失敗", e);
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
                    .filter(a -> !"網路訂票".equals(a.text()))
                    .map(this::toCinemaResponse)
                    .filter(Objects::nonNull);

        } catch (IOException e) {
            return Stream.empty();
        }
    }

    /** 將影城連結轉換為影城資料 */
    private CinemaResponse toCinemaResponse(Element cinemaLink) {
        String cinemaUrl = cinemaLink.absUrl("href");

        if (cinemaUrl.isBlank()) {
            return null;
        }

        String sourceId = getCinemaSourceId(cinemaUrl);
        String cinemaName = cinemaLink.text().trim();

        Element container = cinemaLink.closest("li");

        String cinemaAddress = container != null
                ? getCinemaAddress(container)
                : null;

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

    /** 將取得的資料同步到資料庫 */
    private CinemaResponse syncCinemasToDatabase(CinemaResponse response) {
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

        return cinemaService.findBySourceId(response.sourceId())
                .map(cinema -> cinemaService.updateCinema(
                        cinema.cinemaId(),
                        request))
                .orElseGet(() -> cinemaService.createCinema(request));
    }

    /** 取得影城來源 ID */
    private String getCinemaSourceId(String cinemaUrl) {
        Matcher matcher = CINEMA_ID_PATTERN.matcher(cinemaUrl);

        return matcher.find()
                ? matcher.group(1)
                : null;
    }

    /** 取得影城地址 */
    private String getCinemaAddress(Element container) {
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

        return websiteLink != null
                ? websiteLink.absUrl("href")
                : null;
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
                    .ignoreHttpErrors(true)
                    .get();

            Matcher matcher = COORDINATE_PATTERN.matcher(doc.html());

            if (!matcher.find()) {
                System.out.println("找不到經緯度：" + sourceId + " / " + mapUrl);
                return null;
            }

            return new Coordinates(
                    toCoordinate(matcher.group(1)),
                    toCoordinate(matcher.group(2)));

        } catch (Exception e) {
            System.out.println("經緯度抓取失敗：" + sourceId);
            System.out.println(e.getMessage());
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

        if (address.contains("新竹市")) {
            return County.HSINCHU;
        }

        if (address.contains("新竹縣")) {
            return County.HSINCHU_COUNTY;
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

        if (address.contains("嘉義市")) {
            return County.CHIAYI;
        }

        if (address.contains("嘉義縣")) {
            return County.CHIAYI_COUNTY;
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

        if (address.contains("台東縣")) {
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