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

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.MovieRequest;
import com.collection.benefitsjourney.dto.MovieResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MovieCrawlerService {

    /** 單次網頁請求逾時時間（毫秒）。 */
    private static final int REQUEST_TIMEOUT_MS = 8_000;

    /** 同時下載網頁的最大執行緒數，資料庫操作仍在主執行緒。 */
    private static final int DOWNLOAD_THREAD_COUNT = 4;

    /** 開眼電影網網站根網址。 */
    private static final String BASE_URL = "https://www.atmovies.com.tw";
    private static final String USER_AGENT = "Mozilla/5.0";

    // 只抓固定的最新列表，不再自動往歷史分頁延伸。
    private static final List<String> MOVIE_URLS = List.of(
            BASE_URL + "/movie/new/",
            BASE_URL + "/movie/now/0/",
            BASE_URL + "/movie/now2/0/",
            BASE_URL + "/movie/next/0/");

    /** 僅接受開眼電影網的電影詳情網址，並擷取來源sourceId。 */
    private static final Pattern MOVIE_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?atmovies\\.com\\.tw/(?:movie|film)/(f[A-Za-z0-9]+)/?$",
            Pattern.CASE_INSENSITIVE);

    /** 從中文「上映日期」欄位擷取日期文字。 */
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "上映日期\\s*[：:]\\s*(\\d{1,4}/\\d{1,2}/\\d{1,4})");

    /** 清除片名後方附加的英文名稱（符合指定標題格式時）。 */
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^(.+[\\p{IsHan}」』〉】）])\\s+([A-Za-z].+)$");

    /** 正規化片名時使用的格式。 */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern ATMOVIES_SUFFIX_PATTERN = Pattern.compile(
            "\\s*[@＠]movies.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SITE_SUFFIX_PATTERN = Pattern.compile(
            "\\s*〖開眼電影網〗.*$");

    /** 來源網站可能出現的兩種日期格式。 */
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    };

    /** 爬取過程使用的暫存資料 */
    private record MovieInfo(
            String sourceId,
            String url,
            String movieName,
            String imageUrl,
            LocalDateTime releaseDate) {
    }

    /** 主執行緒取得正式資料，背景任務只下載詳細頁，不操作 JPA。 */
    private record MovieJob(
            MovieInfo info,
            MovieResponse existing,
            Future<MovieInfo> detail) {
    }

    private final MovieService movieService;
    private final CrawlerReviewsService crawlerReviewsService;

    public MovieCrawlerService(
            MovieService movieService,
            CrawlerReviewsService crawlerReviewsService) {
        this.movieService = movieService;
        this.crawlerReviewsService = crawlerReviewsService;
    }

    /** 同步最新電影資料 */
    public List<MovieResponse> syncMovies() {
        long startTime = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(DOWNLOAD_THREAD_COUNT);

        try {
            // 平行抓取四個列表，以 sourceId 合併同一部電影。
            Map<String, MovieInfo> movies = getAllMovies(executor, startTime);

            // 所有列表都抓不到電影時，不應視為正常成功
            if (movies.isEmpty()) {
                log.error("電影爬蟲未取得任何電影資料");

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "電影爬蟲未成功同步任何資料");
            }

            List<MovieResponse> results = new ArrayList<>();
            List<MovieJob> jobs = new ArrayList<>();

            // 逐部查詢正式資料，列表與資料庫皆無法補足的欄位才需要下載詳細頁
            for (MovieInfo info : movies.values()) {
                try {
                    MovieResponse existing = movieService.findBySourceId(info.sourceId()).orElse(null);
                    Future<MovieInfo> detail = needsDetail(info, existing)
                            ? executor.submit(() -> completeMovieInfo(info))
                            : null;

                    jobs.add(new MovieJob(info, existing, detail));
                } catch (Exception e) {
                    log.warn(
                            "電影查詢失敗：{}",
                            info.url(),
                            e);
                }
            }

            // 等待各部電影詳情下載完成，依序寫入資料庫或建立差異審核
            for (MovieJob job : jobs) {
                try {
                    MovieInfo info = job.detail() == null
                            ? job.info()
                            : job.detail().get();

                    MovieResponse existing = job.existing();
                    MovieResponse saved = saveMovie(info, existing);

                    if (saved == null) {
                        continue;
                    }

                    // 新電影直接新增，既有電影不自動覆蓋，而是把有值且不同的欄位送審
                    if (existing != null) {
                        reviewExistingMovie(info, existing);
                    }
                    results.add(saved);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error(
                            "電影同步遭到中斷，耗時 {} 秒",
                            elapsedSeconds(startTime),
                            e);

                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE, "電影同步遭到中斷", e);
                } catch (ExecutionException e) {
                    log.warn(
                            "電影詳細頁處理失敗：{}",
                            job.info().url(),
                            e.getCause() == null ? e : e.getCause());
                } catch (DataIntegrityViolationException e) {
                    log.warn(
                            "電影資料違反資料庫限制，略過同步：{} ({})",
                            job.info().movieName(),
                            job.info().sourceId(),
                            e);
                } catch (Exception e) {
                    log.warn(
                            "電影同步失敗：{}",
                            job.info().url(),
                            e);
                }
            }

            // 有部分電影寫入失敗時留下紀錄，但不影響其他電影
            if (results.size() < movies.size()) {
                log.warn(
                        "部分電影同步資料庫失敗，共抓取 {} 部，成功存入 {} 部，失敗 {} 部",
                        movies.size(),
                        results.size(),
                        movies.size() - results.size());
            }

            // 全部資料都無法處理時，代表同步結果不可視為成功
            if (results.isEmpty()) {
                log.error("所有電影資料皆寫入資料庫失敗");

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "電影爬蟲未成功同步任何資料");
            }

            log.info(
                    "電影爬蟲完成，共抓取 {} 部電影，成功處理 {} 部，耗時 {} 秒",
                    movies.size(),
                    results.size(),
                    elapsedSeconds(startTime));

            return results;
        } finally {
            // 無論同步成功或失敗，都釋放下載用的執行緒
            executor.shutdownNow();
        }
    }

    /** 同時抓四個列表，按照 MOVIE_URLS 的順序合併，避免覆蓋較完整的資料。 */
    private Map<String, MovieInfo> getAllMovies(ExecutorService executor, long startTime) {
        Map<String, Future<Document>> pages = new LinkedHashMap<>();
        Map<String, MovieInfo> movies = new LinkedHashMap<>();

        // 先送出所有下載任務，避免依序下載造成等待
        for (String url : MOVIE_URLS) {
            pages.put(url, executor.submit(() -> connect(url)));
        }

        // 再依固定列表順序解析，讓較前面的來源資料優先保留
        for (Map.Entry<String, Future<Document>> page : pages.entrySet()) {
            try {
                getMovies(page.getValue().get(), movies);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(
                        "電影列表抓取遭到中斷，耗時 {} 秒",
                        elapsedSeconds(startTime),
                        e);

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "電影列表抓取遭到中斷", e);
            } catch (ExecutionException e) {
                logPageFailure(page.getKey(), e.getCause() == null ? e : e.getCause());
            }
        }

        return movies;
    }

    /** 單一列表失敗不影響其他列表，HTTP 狀態與連線失敗分開記錄。 */
    private void logPageFailure(String url, Throwable error) {
        if (error instanceof HttpStatusException httpStatus) {
            log.warn(
                    "電影列表回傳 HTTP {}：{}",
                    httpStatus.getStatusCode(),
                    url,
                    httpStatus);
            return;
        }

        log.warn(
                "電影列表抓取失敗：{}",
                url,
                error);
    }

    /** 只在來源列表、資料庫都拿不到完整資料時，才讀電影詳細頁 */
    private boolean needsDetail(MovieInfo info, MovieResponse existing) {
        if (existing == null) {
            return info.movieName() == null
                    || info.imageUrl() == null
                    || info.releaseDate() == null;
        }

        return info.movieName() == null && existing.movieName() == null
                || info.imageUrl() == null && existing.imageUrl() == null
                || info.releaseDate() == null && existing.releaseDate() == null;
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

    /** 驗證電影網址、擷取欄位，並將相同 sourceId 的非空資訊合併。 */
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

        // 不以後面列表的空值覆蓋先前抓到的有效資料。
        movies.merge(sourceId, info, MovieCrawlerService::mergeMovieInfo);
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
        } catch (HttpStatusException e) {
            log.warn(
                    "電影詳細頁回傳 HTTP {}：{}",
                    e.getStatusCode(),
                    info.url(),
                    e);
            return info;
        } catch (IOException e) {
            log.warn(
                    "電影詳細頁抓取失敗：{}",
                    info.url(),
                    e);
            return info;
        }
    }

    /** 已存在就維持正式資料；首次出現才建立電影，且必須有片名。 */
    private MovieResponse saveMovie(MovieInfo info, MovieResponse existing) {
        if (existing != null) {
            return existing;
        }

        if (info.movieName() == null) {
            log.warn("略過缺少名稱的電影：{}", info.url());
            return null;
        }

        return movieService.createMovie(new MovieRequest(
                info.sourceId(),
                info.movieName(),
                info.imageUrl(),
                true,
                info.releaseDate()));
    }

    /** 只把實際抓到、且與正式資料不同的欄位送審。 */
    private void reviewExistingMovie(MovieInfo info, MovieResponse existing) {
        reviewField(existing.movieId(), CrawlerReviewsService.MOVIE_NAME,
                info.movieName(), existing.movieName());
        reviewField(existing.movieId(), CrawlerReviewsService.IMAGE_URL,
                info.imageUrl(), existing.imageUrl());
        reviewField(existing.movieId(), CrawlerReviewsService.RELEASE_DATE,
                info.releaseDate(), existing.releaseDate());
    }

    /** 欄位有抓到值且和正式資料不同才送審，回傳是否真的新增審核。 */
    private int reviewField(Integer entityId, String fieldName, Object crawled, Object current) {
        if (crawled == null) {
            return 0;
        }

        String crawlerValue = CrawlerReviewsService.formatValue(crawled);
        String previousValue = CrawlerReviewsService.formatValue(current);

        if (Objects.equals(crawlerValue, previousValue)) {
            return 0;
        }

        return crawlerReviewsService.submitCrawlerChange(
                CinemaResourceType.MOVIE,
                entityId,
                fieldName,
                crawlerValue,
                previousValue) ? 1 : 0;
    }

    /** 集中設定 HTTP User-Agent 與逾時；保留 package 可見性方便測試 Spy。 */
    Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MS)
                .get();
    }

    /** 片名優先順序：電影標題區塊 → OG 標題 → 網頁 title。 */
    private String getMovieName(Document doc) {
        String name = cleanMovieName(doc.select(".filmTitle").text());
        if (name != null) {
            return name;
        }

        name = cleanMovieName(doc.select("meta[property=og:title]").attr("content"));
        return name != null ? name : cleanMovieName(doc.title());
    }

    /** 移除網站後綴、壓縮空白，必要時去除英文副標。 */
    private String cleanMovieName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String name = WHITESPACE_PATTERN.matcher(value).replaceAll(" ").trim();
        name = ATMOVIES_SUFFIX_PATTERN.matcher(name).replaceFirst("");
        name = SITE_SUFFIX_PATTERN.matcher(name).replaceFirst("").trim();

        if (name.isBlank() || name.equals("開眼電影網")) {
            return null;
        }

        Matcher matcher = TITLE_PATTERN.matcher(name);
        return matcher.matches() ? matcher.group(1).trim() : name;
    }

    /** 優先取得電影海報，支援延遲載入屬性與 OG 圖片。 */
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

    /** 從頁面文字解析上映日期，日期以當天 00:00 儲存。 */
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

    /** 合併重複電影資料，固定保留先出現的網址與非空欄位。 */
    private static MovieInfo mergeMovieInfo(MovieInfo first, MovieInfo second) {
        return new MovieInfo(
                first.sourceId(),
                first.url(),
                first.movieName() != null ? first.movieName() : second.movieName(),
                first.imageUrl() != null ? first.imageUrl() : second.imageUrl(),
                first.releaseDate() != null ? first.releaseDate() : second.releaseDate());
    }

    private static String elapsedSeconds(long startTime) {
        return "%.2f".formatted((System.nanoTime() - startTime) / 1_000_000_000.0);
    }
}
