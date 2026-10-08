
package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.MovieRequest;
import com.collection.benefitsjourney.dto.MovieResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MovieCrawlerService {

    /** 爬蟲設定 */
    private static final int TIMEOUT = 10000;
    private static final int MAX_PAGES = 100;
    private static final String BASE_URL = "https://www.atmovies.com.tw";

    /** 電影列表來源 */
    private static final List<String> MOVIE_URLS = List.of(
            BASE_URL + "/movie/new/",
            BASE_URL + "/movie/now/",
            BASE_URL + "/movie/now/0/",
            BASE_URL + "/movie/now2/",
            BASE_URL + "/movie/now2/0/",
            BASE_URL + "/movie/next/",
            BASE_URL + "/movie/next/0/");

    /** 電影網址與來源 ID */
    private static final Pattern MOVIE_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?atmovies\\.com\\.tw/(?:movie|film)/(f[A-Za-z0-9]+)/?$",
            Pattern.CASE_INSENSITIVE);

    /** 電影列表分頁 */
    private static final Pattern PAGE_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?atmovies\\.com\\.tw/movie/(?:new|now|now2|next)/(?:\\d+|w\\d+)/?$",
            Pattern.CASE_INSENSITIVE);

    /** 上映日期 */
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "上映日期\\s*[：:]\\s*(\\d{1,4}/\\d{1,2}/\\d{1,4})");

    /** 中文片名與英文原名 */
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^(.+[\\p{IsHan}」』〉】）])\\s+([A-Za-z].+)$");

    /** 日期格式 */
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    };

    /** 暫存爬取的電影資料 */
    private record MovieInfo(
            String sourceId,
            String url,
            String movieName,
            String imageUrl,
            LocalDateTime releaseDate) {
    }

    private final MovieService movieService;

    public MovieCrawlerService(MovieService movieService) {
        this.movieService = movieService;
    }

    /** 同步所有電影資料 */
    public List<MovieResponse> syncMovies() {
        long start = System.nanoTime();

        Map<String, MovieInfo> movies = getAllMovies();
        List<MovieResponse> results = new ArrayList<>();

        for (MovieInfo info : movies.values()) {
            try {
                MovieResponse movie = saveMovie(info);

                if (movie != null) {
                    results.add(movie);
                }

            } catch (Exception e) {
                log.warn("電影同步失敗：{}，原因：{}",
                        info.url(), e.getMessage());
            }
        }

        if (results.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "電影爬蟲未成功同步任何資料");
        }

        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

        log.info("電影爬蟲完成：找到 {} 部，成功 {} 部，失敗 {} 部，耗時 {} 秒",
                movies.size(),
                results.size(),
                movies.size() - results.size(),
                String.format("%.2f", seconds));

        return results;
    }

    /** 抓取電影列表與分頁 */
    private Map<String, MovieInfo> getAllMovies() {
        Map<String, MovieInfo> movies = new LinkedHashMap<>();

        Deque<String> pending = new ArrayDeque<>(MOVIE_URLS);
        Set<String> seen = new HashSet<>(MOVIE_URLS);

        int checked = 0;

        while (!pending.isEmpty() && checked < MAX_PAGES) {
            String url = pending.remove();
            checked++;

            try {
                Document doc = connect(url);

                getMovies(doc, movies);
                getPageUrls(doc, pending, seen);

                log.info("電影列表：{}，目前共 {} 部",
                        url, movies.size());

            } catch (IOException e) {
                log.warn("電影列表抓取失敗：{}，原因：{}",
                        url, e.getMessage());
            }
        }

        if (!pending.isEmpty()) {
            log.warn("已達電影列表頁數上限：{}", MAX_PAGES);
        }

        return movies;
    }

    /** 取得列表中的電影 */
    private void getMovies(
            Document doc,
            Map<String, MovieInfo> movies) {

        // 一般電影列表
        for (Element article : doc.select("article.box.post")) {
            Element link = article.selectFirst(".filmTitle a[href]");

            if (link != null) {
                addMovie(movies, link, article);
            }
        }

        // 快覽頁或其他電影連結
        for (Element link : doc.select("a[href]")) {
            addMovie(movies, link, null);
        }
    }

    /** 新增或合併電影資料 */
    private void addMovie(
            Map<String, MovieInfo> movies,
            Element link,
            Element article) {

        String url = link.absUrl("href");
        Matcher matcher = MOVIE_PATTERN.matcher(url);

        if (!matcher.matches()) {
            return;
        }

        String sourceId = matcher.group(1);

        MovieInfo info = new MovieInfo(
                sourceId,
                url,
                cleanMovieName(link.text()),
                article == null ? null : getImageUrl(article),
                article == null ? null : getReleaseDate(article));

        movies.merge(sourceId, info, (old, next) -> new MovieInfo(
                old.sourceId(),
                old.url(),
                old.movieName() != null
                        ? old.movieName()
                        : next.movieName(),
                old.imageUrl() != null
                        ? old.imageUrl()
                        : next.imageUrl(),
                old.releaseDate() != null
                        ? old.releaseDate()
                        : next.releaseDate()));
    }

    /** 取得其他電影列表分頁 */
    private void getPageUrls(
            Document doc,
            Deque<String> pending,
            Set<String> seen) {

        for (Element link : doc.select("a[href]")) {
            String url = link.absUrl("href");

            if (PAGE_PATTERN.matcher(url).matches()
                    && seen.add(url)) {
                pending.add(url);
            }
        }
    }

    /** 新增或更新電影資料庫 */
    private MovieResponse saveMovie(MovieInfo info) {
        MovieInfo movie = completeMovieInfo(info);

        if (movie.movieName() == null) {
            log.warn("缺少電影名稱，略過：{}", movie.url());
            return null;
        }

        MovieResponse existing = movieService
                .findBySourceId(movie.sourceId())
                .orElse(null);

        String imageUrl = movie.imageUrl();
        LocalDateTime releaseDate = movie.releaseDate();

        // 爬蟲未取得的欄位，保留資料庫原本的值
        if (existing != null) {
            if (imageUrl == null) {
                imageUrl = existing.imageUrl();
            }

            if (releaseDate == null) {
                releaseDate = existing.releaseDate();
            }
        }

        // 資料沒變化就不更新
        if (existing != null
                && movie.movieName().equals(existing.movieName())
                && Objects.equals(imageUrl, existing.imageUrl())
                && Objects.equals(releaseDate, existing.releaseDate())) {
            return existing;
        }

        // 保留原本的顯示設定
        boolean isVisible = existing == null
                || Boolean.TRUE.equals(existing.isVisible());

        MovieRequest request = new MovieRequest(
                movie.sourceId(),
                movie.movieName(),
                imageUrl,
                isVisible,
                releaseDate);

        if (existing == null) {
            return movieService.createMovie(request);
        }

        return movieService.updateMovie(
                existing.movieId(), request);
    }

    /** 列表資料不足時讀取詳細頁 */
    private MovieInfo completeMovieInfo(MovieInfo info) {
        if (info.movieName() != null
                && info.imageUrl() != null
                && info.releaseDate() != null) {
            return info;
        }

        try {
            Document doc = connect(info.url());

            return new MovieInfo(
                    info.sourceId(),
                    info.url(),
                    info.movieName() != null
                            ? info.movieName()
                            : getMovieName(doc),
                    info.imageUrl() != null
                            ? info.imageUrl()
                            : getImageUrl(doc),
                    info.releaseDate() != null
                            ? info.releaseDate()
                            : getReleaseDate(doc.body()));

        } catch (IOException e) {
            log.warn("電影詳細頁抓取失敗：{}，原因：{}",
                    info.url(), e.getMessage());

            return info;
        }
    }

    /** 連線至來源網站 */
    private Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(TIMEOUT)
                .get();
    }

    /** 取得電影名稱 */
    private String getMovieName(Document doc) {
        String name = cleanMovieName(
                doc.select(".filmTitle").text());

        if (name != null) {
            return name;
        }

        name = cleanMovieName(
                doc.select("meta[property=og:title]").attr("content"));

        return name != null
                ? name
                : cleanMovieName(doc.title());
    }

    /** 清理電影名稱 */
    private String cleanMovieName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String name = value.replaceAll("\\s+", " ").trim()
                .replaceFirst("\\s*[@＠]movies.*$", "")
                .replaceFirst("\\s*〖開眼電影網〗.*$", "")
                .trim();

        if (name.isBlank() || name.equals("開眼電影網")) {
            return null;
        }

        Matcher matcher = TITLE_PATTERN.matcher(name);

        return matcher.matches()
                ? matcher.group(1).trim()
                : name;
    }

    /** 取得電影海報 */
    private String getImageUrl(Element root) {
        Element image = root.selectFirst(
                ".filmImg img, .filmPic img, .poster img");

        if (image == null && root.tagName().equalsIgnoreCase("article")) {
            image = root.selectFirst("img[src], img[data-src]");
        }

        if (image != null) {
            for (String attr : List.of("data-src", "data-original", "src")) {
                String url = image.absUrl(attr);

                if (url.startsWith("http")) {
                    return url;
                }
            }
        }

        Element meta = root.selectFirst("meta[property=og:image]");

        if (meta != null) {
            String url = meta.absUrl("content");

            if (url.startsWith("http")) {
                return url;
            }
        }

        return null;
    }

    /** 取得上映日期 */
    private LocalDateTime getReleaseDate(Element root) {
        Matcher matcher = DATE_PATTERN.matcher(root.text());

        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1);

        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format)
                        .atStartOfDay();

            } catch (DateTimeParseException ignored) {
                // 嘗試其他日期格式
            }
        }

        log.warn("無法解析上映日期：{}", value);
        return null;
    }
}
