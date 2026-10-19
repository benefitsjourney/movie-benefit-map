package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 呼叫Apify的Facebook Posts Scraper，直接拿回貼文JSON。
 *
 * 使用「同步」API：Apify跑完才回應，單次最多等300秒（貼文數100篇以內通常夠用）。
 * Token只從環境變數APIFY_TOKEN（或apify.token）讀取。
 */
@Slf4j
@Service
public class ApifyClient {

    private static final String ENDPOINT_PREFIX = "https://api.apify.com/v2/actors/";
    private static final String ENDPOINT_SUFFIX = "/run-sync-get-dataset-items";

    /** 同步API最多只等300秒，超過Apify會回408 */
    private static final int MAX_RUN_SECONDS = 280;

    /** 讀apify.token或環境變數APIFY_TOKEN（兩個都沒設就是空字串） */
    @Value("${apify.token:${APIFY_TOKEN:}}")
    private String token;

    /** Apify上的爬蟲名稱。讀apify.facebook-actor或環境變數APIFY_FACEBOOK_ACTOR，例如apify/facebook-posts-scraper */
    @Value("${apify.facebook-actor:${APIFY_FACEBOOK_ACTOR:apify/facebook-posts-scraper}}")
    private String actor;

    @Value("${apify.timeout-seconds:" + MAX_RUN_SECONDS + "}")
    private int timeoutSeconds;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param pageUrl 　 粉專網址，例如 https://www.facebook.com/vieshow
     * @param limit   　 最多抓幾篇（由新到舊）
     * @param sinceDays　只抓最近幾天內的貼文，0或負數＝不限（只靠limit）
     * @return 貼文陣列（每篇有postId、url、time、text、facebookUrl、pageName…）
     */
    public JsonNode fetchPosts(String pageUrl, int limit, int sinceDays) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "尚未設定APIFY_TOKEN環境變數，無法呼叫Apify");
        }

        int runSeconds = Math.min(Math.max(timeoutSeconds, 30), MAX_RUN_SECONDS);
        String since = sinceDays > 0 ? ",\"onlyPostsNewerThan\":\"" + sinceDays + " days\"" : "";
        String body = "{\"startUrls\":[{\"url\":" + quote(pageUrl) + "}],\"resultsLimit\":" + limit + since + "}";
        // API網址的Actor名稱，擁有者與名稱之間要用~（apify/xxx→apify~xxx）
        String endpoint = ENDPOINT_PREFIX + actor.trim().replace('/', '~') + ENDPOINT_SUFFIX;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "?clean=true&timeout=" + runSeconds))
                .timeout(Duration.ofSeconds(runSeconds + 20L))   // 比Apify多等一下，避免斷線
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        log.info("呼叫Apify：{}，最多 {} 篇，{}", pageUrl, limit, sinceDays > 0 ? "最近 " + sinceDays + " 天" : "不限天數");
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Apify呼叫被中斷", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "無法連線至Apify：" + e.getMessage(), e);
        }

        int status = response.statusCode();
        if (status != 200 && status != 201) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, describeError(status, response.body()));
        }

        try {
            JsonNode posts = mapper.readTree(response.body());
            if (posts == null || !posts.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apify回傳的內容不是貼文陣列");
            }
            log.info("Apify回傳 {} 筆資料", posts.size());
            return posts;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apify回傳的JSON無法解析", e);
        }
    }

    /** 常見錯誤 */
    private String describeError(int status, String body) {
        return switch (status) {
            case 401 -> "Apify Token無效，請確認APIFY_TOKEN 是否正確";
            case 402 -> "Apify額度不足";
            case 408 -> "Apify執行超過300秒，請調小limit再試";
            case 429 -> "Apify呼叫太頻繁，請稍後再試";
            default -> "Apify回應錯誤HTTP " + status + "：" + abbreviate(body);
        };
    }

    private String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }

    /** 把網址包成JSON字串（處理引號與反斜線） */
    private String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}