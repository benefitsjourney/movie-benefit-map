package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CrawlerSyncResult;
import com.collection.benefitsjourney.dto.MovieRequest;
import com.collection.benefitsjourney.dto.MovieResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CrawlerMovieService {

    private static final String BASE_URL = "https://www.atmovies.com.tw";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36";
    private static final int TIMEOUT_MS = 12_000;
    private static final int THREADS = 6;
    private static final int NAME_MAX_LENGTH = 64;

    private static final List<String> ENTRY_URLS = List.of(
            BASE_URL + "/movie/new/",
            BASE_URL + "/movie/now/0/",
            BASE_URL + "/movie/now2/0/",
            BASE_URL + "/movie/next/0/",
            BASE_URL + "/movie/next2/");

    private static final Pattern MOVIE_LINK = Pattern.compile(
            "^https?://(?:www\\.)?atmovies\\.com\\.tw/(?:movie|film)/(f[A-Za-z0-9]+)/?(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WEEK_LINK = Pattern.compile("/movie/next/w\\d+/?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEASE_DATE = Pattern.compile("上映日期\\s*[：:]\\s*(\\d{1,4}/\\d{1,2}/\\d{1,4})");
    private static final Pattern WEBSITE_SUFFIX = Pattern.compile(
            "\\s*(?:[-–—|｜]\\s*)?(?:[@＠]movies|〖開眼電影網〗|開眼電影網)(?:.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CHINESE = Pattern.compile("[\\p{IsHan}]");
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static final Pattern ENGLISH_TRANSLATION = Pattern.compile(
            "^(.+?)\\s+([A-Za-z][A-Za-z0-9 .=,!?:;’'&+()\\-/–—！]+)$");
    private static final Pattern ENGLISH_WITH_DESCRIPTION = Pattern.compile(
            "^(.+?)\\s+[A-Za-z][A-Za-z0-9 .=,!?'&+()\\-/–—]*[：:]\\s*[\\p{IsHan}].*$");
    private static final Pattern EDITION_SUFFIX = Pattern.compile(
            "\\s*[（(《〈\u3010]\\s*(?:真人版|數位修復版|數字修復版|修復版|[24]K修復版)\\s*[）)》〉\u3011]$");
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    };

    private record MovieInfo(String sourceId, String url, String movieName,
                             String imageUrl, LocalDateTime releaseDate) {}

    private record StoredMovie(Integer id, String sourceId, String name) {}

    private final MovieService movieService;
    private final CrawlerReviewsService crawlerReviewsService;
    private final JdbcTemplate jdbcTemplate;

    public CrawlerMovieService(MovieService movieService,
                               CrawlerReviewsService crawlerReviewsService,
                               JdbcTemplate jdbcTemplate) {
        this.movieService = movieService;
        this.crawlerReviewsService = crawlerReviewsService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 列表找影片，詳細頁確認完整名稱及資料。 */
    public CrawlerSyncResult<MovieResponse> syncMovies() {
        long started = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            Map<String, Future<Document>> pages = new LinkedHashMap<>();
            for (String url : ENTRY_URLS) {
                pages.put(url, executor.submit(() -> connect(url)));
            }

            Map<String, MovieInfo> discovered = new LinkedHashMap<>();
            Set<String> weeks = new LinkedHashSet<>();
            for (var entry : pages.entrySet()) {
                Document doc = await(entry.getValue(), entry.getKey());
                if (doc == null) continue;
                discover(doc, discovered);
                if (entry.getKey().contains("/next/0/")) {
                    for (Element link : doc.select("a[href]")) {
                        String url = link.absUrl("href");
                        if (WEEK_LINK.matcher(url).find()) weeks.add(url);
                    }
                }
            }

            Map<String, Future<Document>> weeklyJobs = new LinkedHashMap<>();
            for (String url : weeks) weeklyJobs.put(url, executor.submit(() -> connect(url)));
            for (var entry : weeklyJobs.entrySet()) {
                Document doc = await(entry.getValue(), entry.getKey());
                if (doc == null) continue;
                discover(doc, discovered);
            }

            if (discovered.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "電影網站沒有回傳可用列表");
            }

            Map<String, Future<MovieInfo>> details = new LinkedHashMap<>();
            for (MovieInfo item : discovered.values()) {
                details.put(item.sourceId(), executor.submit(() -> fetchDetail(item)));
            }

            Map<String, StoredMovie> names = loadStoredMovies();
            List<MovieResponse> results = new ArrayList<>();
            int fetched = 0;
            int created = 0;
            int skipped = 0;
            int updated = 0;
            int reviewed = 0;
            int duplicates = 0;

            for (MovieInfo candidate : discovered.values()) {
                MovieInfo info = await(details.get(candidate.sourceId()), candidate.url());
                if (info == null || info.movieName() == null) {
                    skipped++;
                    continue;
                }
                fetched++;
                try {
                    MovieResponse existing = movieService.findBySourceId(info.sourceId()).orElse(null);
                    if (existing != null) {
                        int reviewedFields = reviewExisting(info, existing);
                        reviewed += reviewedFields;
                        if (reviewedFields > 0) {
                            updated++;
                        }
                        results.add(existing);
                        continue;
                    }

                    StoredMovie duplicate = names.get(normalizeName(info.movieName()));
                    if (duplicate != null) {
                        duplicates++;
                        if (!Objects.equals(duplicate.sourceId(), info.sourceId())
                                && submitDuplicateReview(info, duplicate)) {
                            reviewed++;
                        }
                        continue;
                    }

                    MovieResponse saved = movieService.createMovie(new MovieRequest(
                            info.sourceId(), info.movieName(), info.imageUrl(), true, info.releaseDate()));
                    results.add(saved);
                    created++;
                    names.put(normalizeName(info.movieName()),
                            new StoredMovie(saved.movieId(), info.sourceId(), info.movieName()));
                } catch (Exception e) {
                    log.warn("電影同步失敗：{} ({})", info.url(), info.movieName(), e);
                }
            }

            if (results.isEmpty() && duplicates == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "沒有任何電影同步成功");
            }
            CrawlerSyncResult<MovieResponse> result = CrawlerSyncResult.of(
                    fetched, created, skipped, updated, reviewed, elapsedSeconds(started), results);
            log.info("電影同步完成：{}", result.message());
            return result;
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> T await(Future<T> job, String url) {
        try {
            return job.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "電影爬蟲執行遭中斷", e);
        } catch (ExecutionException e) {
            log.warn("電影頁面抓取失敗：{}", url, e.getCause());
            return null;
        }
    }

    private void discover(Document page, Map<String, MovieInfo> found) {
        for (Element link : page.select("a[href]")) {
            Matcher match = MOVIE_LINK.matcher(link.absUrl("href"));
            if (!match.matches()) continue;
            String id = match.group(1);
            found.putIfAbsent(id, new MovieInfo(id, BASE_URL + "/movie/" + id + "/",
                    null, null, null));
        }
    }

    private MovieInfo fetchDetail(MovieInfo item) throws IOException {
        Document doc = connect(item.url());
        String name = movieTitle(doc);
        if (name == null) {
            log.warn("電影詳細頁缺少完整有效中文片名，略過：{}", item.url());
            return item;
        }
        return new MovieInfo(item.sourceId(), item.url(), name,
                BASE_URL + "/photo101/" + item.sourceId() + "/pl_" + item.sourceId() + ".jpg",
                releaseDate(doc));
    }

    /** 網站頁籤的正式名稱最穩定；不得把劇情簡介拼進片名。 */
    private String movieTitle(Document doc) {
        // 開眼格式：「中文片名 English Title @movies〖開眼電影網〗」
        String pageTitle = doc.title();
        String title = WEBSITE_SUFFIX.matcher(pageTitle).find() ? cleanTitle(pageTitle) : null;
        if (title != null) return title;

        // 網站頁籤缺失時，才使用電影標題欄位；不讀取其父層的介紹文字。
        for (String selector : List.of(".filmTitle a", "meta[property=og:title]")) {
            Element element = doc.selectFirst(selector);
            if (element == null) continue;
            String raw = element.tagName().equals("meta") ? element.attr("content") : element.ownText();
            title = cleanTitle(raw);
            if (title != null) return title;
        }
        return null;
    }

    /** 保留中文正式片名及副標題，只移除後方獨立的英文譯名。 */
    private String cleanTitle(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String title = raw.replace('\u00a0', ' ').strip().split("[\r\n]+", 2)[0];
        title = WEBSITE_SUFFIX.matcher(title).replaceFirst("");
        title = title.replaceFirst("\\s*[-–—|｜]\\s*(?:電影介紹|劇情介紹|劇情簡介).*$", "");
        title = SPACE.matcher(title).replaceAll(" ").trim();

        // 詳細頁可能是「中文正式名稱 English Title：劇情文字」。
        // 從中英文交界拆開，而不是從冒號或片名中的數字、英文拆開。
        title = stripEnglishTranslation(title);

        if (title.isEmpty() || title.length() > NAME_MAX_LENGTH || !CHINESE.matcher(title).find()
                || title.equals("電影") || title.equals("開眼電影網")) return null;
        return title;
    }

    private String stripEnglishTranslation(String title) {
        // 譯名必須以空格後的英文單字起頭，且中文名稱部分確實含中文字。
        // 允許中文名稱以 2、0.0、N.E.W.、2+2=5、半形括號等形式結尾。
        Matcher description = ENGLISH_WITH_DESCRIPTION.matcher(title);
        if (description.matches() && CHINESE.matcher(description.group(1)).find()) {
            return description.group(1).trim();
        }
        Matcher suffix = ENGLISH_TRANSLATION.matcher(title);
        if (suffix.matches() && CHINESE.matcher(suffix.group(1)).find()) {
            return suffix.group(1).trim();
        }
        return title;
    }

    /** 網站略去版本註記時，避免把現有「真人版／修復版」改成一般電影名稱。 */
    private boolean sameTitleIgnoringEdition(String crawled, String stored) {
        if (crawled == null || stored == null) return false;
        String withoutEdition = EDITION_SUFFIX.matcher(stored.trim()).replaceFirst("").trim();
        return !withoutEdition.equals(stored.trim())
                && normalizeName(crawled).equals(normalizeName(withoutEdition));
    }

    private LocalDateTime releaseDate(Document doc) {
        Matcher match = RELEASE_DATE.matcher(doc.body().text());
        if (!match.find()) return null;
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(match.group(1), formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // 日期來源有兩種格式。
            }
        }
        return null;
    }

    private String normalizeName(String raw) {
        return Normalizer.normalize(raw, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{Z}\\s]+", "");
    }

    private Map<String, StoredMovie> loadStoredMovies() {
        Map<String, StoredMovie> names = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT movie_id, source_id, movie_name FROM movie", rs -> {
            StoredMovie movie = new StoredMovie(rs.getInt("movie_id"),
                    rs.getString("source_id"), rs.getString("movie_name"));
            if (movie.name() != null) names.putIfAbsent(normalizeName(movie.name()), movie);
        });
        return names;
    }

    private boolean submitDuplicateReview(MovieInfo info, StoredMovie duplicate) {
        log.warn("同名電影待審核：{}，現有sourceId={}，新sourceId={}",
                info.movieName(), duplicate.sourceId(), info.sourceId());
        return crawlerReviewsService.submitCrawlerChange(CinemaResourceType.MOVIE, duplicate.id(),
                CrawlerReviewsService.SOURCE_ID, info.sourceId(), duplicate.sourceId());
    }

    private int reviewExisting(MovieInfo info, MovieResponse existing) {
        int reviewed = 0;
        if (!sameTitleIgnoringEdition(info.movieName(), existing.movieName())
                && reviewField(existing.movieId(), CrawlerReviewsService.MOVIE_NAME,
                        info.movieName(), existing.movieName())) {
            reviewed++;
        }
        if (reviewField(existing.movieId(), CrawlerReviewsService.IMAGE_URL,
                info.imageUrl(), existing.imageUrl())) {
            reviewed++;
        }
        if (reviewField(existing.movieId(), CrawlerReviewsService.RELEASE_DATE,
                info.releaseDate(), existing.releaseDate())) {
            reviewed++;
        }
        return reviewed;
    }

    private boolean reviewField(Integer id, String field, Object crawled, Object current) {
        if (crawled == null) return false;
        String after = CrawlerReviewsService.formatValue(crawled);
        String before = CrawlerReviewsService.formatValue(current);
        if (Objects.equals(after, before)) return false;
        return crawlerReviewsService.submitCrawlerChange(CinemaResourceType.MOVIE, id, field, after, before);
    }

    private static long elapsedSeconds(long started) {
        return (System.nanoTime() - started) / 1_000_000_000L;
    }

    Document connect(String url) throws IOException {
        return Jsoup.connect(url).userAgent(USER_AGENT).referrer(BASE_URL + "/")
                .timeout(TIMEOUT_MS).followRedirects(true).get();
    }
}
