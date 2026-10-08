package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private static final int TIMEOUT = 8000;
    private static final int THREADS = 4;
    private static final String BASE_URL = "https://www.atmovies.com.tw";

    // 只抓固定的最新列表，不再自動往歷史分頁延伸。
    private static final List<String> MOVIE_URLS = List.of(
            BASE_URL + "/movie/new/",
            BASE_URL + "/movie/now/0/",
            BASE_URL + "/movie/now2/0/",
            BASE_URL + "/movie/next/0/");

    private static final Pattern MOVIE_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?atmovies\\.com\\.tw/(?:movie|film)/(f[A-Za-z0-9]+)/?$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "上映日期\\s*[：:]\\s*(\\d{1,4}/\\d{1,2}/\\d{1,4})");

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^(.+[\\p{IsHan}」』〉】）])\\s+([A-Za-z].+)$");

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    };

    private record MovieInfo(
            String sourceId,
            String url,
            String movieName,
            String imageUrl,
            LocalDateTime releaseDate) {
    }

    // existing 在主執行緒查詢；detail 只負責網路請求，避免背景執行緒操作 JPA。
    private record MovieJob(
            MovieInfo info,
            MovieResponse existing,
            Future<MovieInfo> detail) {
    }

    private final MovieService movieService;

    public MovieCrawlerService(MovieService movieService) {
        this.movieService = movieService;
    }

    /** 同步最新電影資料（列表與必要的詳細頁平行抓取，資料庫依序寫入）。 */
    public List<MovieResponse> syncMovies() {
        long start = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try {
            Map<String, MovieInfo> movies = getAllMovies(executor);
            List<MovieResponse> results = new ArrayList<>();
            List<MovieJob> jobs = new ArrayList<>();
            int details = 0;
            int failed = 0;

            // 先查已有資料，僅有缺漏資料的電影才排入詳細頁請求。
            for (MovieInfo info : movies.values()) {
                try {
                    MovieResponse existing = movieService.findBySourceId(info.sourceId()).orElse(null);
                    Future<MovieInfo> detail = null;

                    if (needsDetail(info, existing)) {
                        detail = executor.submit(() -> completeMovieInfo(info));
                        details++;
                    }

                    jobs.add(new MovieJob(info, existing, detail));
                } catch (Exception e) {
                    failed++;
                    log.warn("電影查詢失敗：{}，原因：{}", info.url(), e.getMessage());
                }
            }

            int created = 0;
            int updated = 0;
            int unchanged = 0;

            // 詳細頁同時進行下載；資料庫只在這裡依序新增／修改。
            for (MovieJob job : jobs) {
                try {
                    MovieInfo info = job.detail() == null
                            ? job.info()
                            : job.detail().get();

                    MovieResponse existing = job.existing();
                    boolean same = isUnchanged(info, existing);
                    MovieResponse saved = saveMovie(info, existing);

                    if (saved == null) {
                        failed++;
                        continue;
                    }

                    results.add(saved);
                    if (existing == null) {
                        created++;
                    } else if (same) {
                        unchanged++;
                    } else {
                        updated++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE, "電影同步遭到中斷", e);
                } catch (ExecutionException e) {
                    failed++;
                    log.warn("電影詳細頁處理失敗：{}，原因：{}",
                            job.info().url(), e.getCause().getMessage());
                } catch (Exception e) {
                    failed++;
                    log.warn("電影同步失敗：{}，原因：{}", job.info().url(), e.getMessage());
                }
            }

            if (results.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "電影爬蟲未成功同步任何資料");
            }

            log.info("電影爬蟲完成：列表 {} 頁、電影 {} 部、詳細頁 {} 頁、新增 {} 部、更新 {} 部、未變 {} 部、失敗 {} 部、耗時 {} 秒",
                    MOVIE_URLS.size(), movies.size(), details,
                    created, updated, unchanged, failed,
                    String.format("%.2f", (System.nanoTime() - start) / 1_000_000_000.0));

            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /** 同時抓四個列表，按照 MOVIE_URLS 的順序合併，避免覆蓋較完整的資料。 */
    private Map<String, MovieInfo> getAllMovies(ExecutorService executor) {
        Map<String, Future<Document>> pages = new LinkedHashMap<>();
        Map<String, MovieInfo> movies = new LinkedHashMap<>();

        for (String url : MOVIE_URLS) {
            pages.put(url, executor.submit(() -> connect(url)));
        }

        for (Map.Entry<String, Future<Document>> page : pages.entrySet()) {
            long start = System.nanoTime();
            try {
                int before = movies.size();
                getMovies(page.getValue().get(), movies);
                log.info("電影列表：{}，新增 {} 部，累計 {} 部，處理耗時 {} 秒",
                        page.getKey(), movies.size() - before, movies.size(),
                        String.format("%.2f", (System.nanoTime() - start) / 1_000_000_000.0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "電影列表抓取遭到中斷", e);
            } catch (ExecutionException e) {
                log.warn("電影列表抓取失敗：{}，原因：{}",
                        page.getKey(), e.getCause().getMessage());
            }
        }

        return movies;
    }

    /** 只在來源列表、資料庫都拿不到完整資料時，才讀電影詳細頁。 */
    private boolean needsDetail(MovieInfo info, MovieResponse existing) {
        boolean hasName = info.movieName() != null
                || (existing != null && existing.movieName() != null);
        boolean hasImage = info.imageUrl() != null
                || (existing != null && existing.imageUrl() != null);
        boolean hasDate = info.releaseDate() != null
                || (existing != null && existing.releaseDate() != null);

        return !hasName || !hasImage || !hasDate;
    }

    /** 取得列表中的電影，同一 sourceId 只保留一份。 */
    private void getMovies(Document doc, Map<String, MovieInfo> movies) {
        for (Element article : doc.select("article.box.post")) {
            Element link = article.selectFirst(".filmTitle a[href]");
            if (link != null) {
                addMovie(movies, link, article);
            }
        }

        // /now/0/、/now2/0/、/next/0/ 是快覽頁，只有電影連結。
        for (Element link : doc.select("a[href]")) {
            addMovie(movies, link, null);
        }
    }

    private void addMovie(Map<String, MovieInfo> movies, Element link, Element article) {
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
                old.movieName() != null ? old.movieName() : next.movieName(),
                old.imageUrl() != null ? old.imageUrl() : next.imageUrl(),
                old.releaseDate() != null ? old.releaseDate() : next.releaseDate()));
    }

    /** 優先使用列表資料；缺漏才從詳細頁補足。 */
    private MovieInfo completeMovieInfo(MovieInfo info) {
        try {
            Document doc = connect(info.url());
            return new MovieInfo(
                    info.sourceId(),
                    info.url(),
                    info.movieName() != null ? info.movieName() : getMovieName(doc),
                    info.imageUrl() != null ? info.imageUrl() : getImageUrl(doc),
                    info.releaseDate() != null ? info.releaseDate() : getReleaseDate(doc.body()));
        } catch (IOException e) {
            log.warn("電影詳細頁抓取失敗：{}，原因：{}", info.url(), e.getMessage());
            return info;
        }
    }

    private MovieResponse saveMovie(MovieInfo info, MovieResponse existing) {
        String movieName = info.movieName() != null
                ? info.movieName()
                : existing == null ? null : existing.movieName();

        if (movieName == null) {
            log.warn("缺少電影名稱，略過：{}", info.url());
            return null;
        }

        // 未取得的新值，不覆蓋原本資料庫的內容。
        String imageUrl = info.imageUrl() != null
                ? info.imageUrl()
                : existing == null ? null : existing.imageUrl();
        LocalDateTime releaseDate = info.releaseDate() != null
                ? info.releaseDate()
                : existing == null ? null : existing.releaseDate();

        if (existing != null
                && movieName.equals(existing.movieName())
                && Objects.equals(imageUrl, existing.imageUrl())
                && Objects.equals(releaseDate, existing.releaseDate())) {
            return existing;
        }

        boolean isVisible = existing == null || Boolean.TRUE.equals(existing.isVisible());
        MovieRequest request = new MovieRequest(
                info.sourceId(), movieName, imageUrl, isVisible, releaseDate);

        return existing == null
                ? movieService.createMovie(request)
                : movieService.updateMovie(existing.movieId(), request);
    }

    private boolean isUnchanged(MovieInfo info, MovieResponse existing) {
        if (existing == null) {
            return false;
        }

        String name = info.movieName() != null ? info.movieName() : existing.movieName();
        String image = info.imageUrl() != null ? info.imageUrl() : existing.imageUrl();
        LocalDateTime date = info.releaseDate() != null ? info.releaseDate() : existing.releaseDate();

        return Objects.equals(name, existing.movieName())
                && Objects.equals(image, existing.imageUrl())
                && Objects.equals(date, existing.releaseDate());
    }

    private Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(TIMEOUT)
                .get();
    }

    private String getMovieName(Document doc) {
        String name = cleanMovieName(doc.select(".filmTitle").text());
        if (name != null) {
            return name;
        }

        name = cleanMovieName(doc.select("meta[property=og:title]").attr("content"));
        return name != null ? name : cleanMovieName(doc.title());
    }

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
        return matcher.matches() ? matcher.group(1).trim() : name;
    }

    private String getImageUrl(Element root) {
        Element image = root.selectFirst(".filmImg img, .filmPic img, .poster img");

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

    private LocalDateTime getReleaseDate(Element root) {
        Matcher matcher = DATE_PATTERN.matcher(root.text());
        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1);
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // 換一種日期格式解析。
            }
        }

        log.warn("無法解析上映日期：{}", value);
        return null;
    }
}
