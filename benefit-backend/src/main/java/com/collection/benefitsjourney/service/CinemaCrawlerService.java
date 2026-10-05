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

        return new CinemaCrawlerResponse(
                sourceId,
                cinemaName,
                cinemaAddress,
                null,
                null,
                officialWebsiteUrl,
                null,
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
}