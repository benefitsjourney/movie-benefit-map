package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.dto.CinemaCrawlerResponse;
import com.collection.benefitsjourney.enums.County;

@Service
public class CinemaCrawlerService {

    private static final String ATMOVIES_SHOWTIME_URL = "https://www.atmovies.com.tw/showtime/";

    /** 抓取所有影城資料 */
    public List<CinemaCrawlerResponse> syncCinemas() {
        try {
            return Jsoup.connect(ATMOVIES_SHOWTIME_URL)
                    .userAgent("Mozilla/5.0")
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

        } catch (IOException e) {
            throw new RuntimeException("抓取影城資料失敗", e);
        }
    }

    /** 抓取指定縣市的影城資料 */
    private Stream<CinemaCrawlerResponse> getCinemasByAreaUrl(String areaUrl) {
        try {
            Document doc = Jsoup.connect(areaUrl)
                    .userAgent("Mozilla/5.0")
                    .get();

            return doc.select("a[href^=/showtime/t]")
                    .stream()
                    .filter(a -> !a.text().isBlank())
                    .filter(a -> !"網路訂票".equals(a.text()))
                    .map(this::toCinemaCrawlerResponse)
                    .filter(Objects::nonNull);

        } catch (IOException e) {
            return Stream.empty();
        }
    }

    /** 將影城連結轉換為影城爬蟲資料 */
    private CinemaCrawlerResponse toCinemaCrawlerResponse(Element cinemaLink) {
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

        String officialWebsiteUrl = getOfficialWebsiteUrl(cinemaUrl);

        County county = getCounty(cinemaAddress);

        return new CinemaCrawlerResponse(
                sourceId,
                cinemaName,
                cinemaAddress,
                null,
                null,
                officialWebsiteUrl,
                county,
                true);
    }

    /** 取得影城來源 ID */
    private String getCinemaSourceId(String cinemaUrl) {
        String[] parts = cinemaUrl.split("/");

        if (parts.length > 4) {
            return parts[4];
        }

        return null;
    }

    /** 取得影城地址 */
    private String getCinemaAddress(Element container) {
        Element addressElement = container.selectFirst("ul li");

        return addressElement != null
                ? addressElement.text().trim()
                : null;
    }

    /** 取得影城官方網站 */
    private String getOfficialWebsiteUrl(String cinemaUrl) {
        try {
            Document doc = Jsoup.connect(cinemaUrl)
                    .userAgent("Mozilla/5.0")
                    .get();

            Element websiteLink = doc.select("a[href]")
                    .stream()
                    .filter(a -> {
                        Element parent = a.parent();
                        return parent != null && parent.text().contains("網站");
                    })
                    .findFirst()
                    .orElse(null);

            return websiteLink != null
                    ? websiteLink.absUrl("href")
                    : null;

        } catch (IOException e) {
            return null;
        }
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