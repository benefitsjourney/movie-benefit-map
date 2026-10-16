package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 呼叫Apify Facebook貼文Actor。
 *
 * 【實際 HTTP 請求預設停用】會消耗 Apify 免費額度。
 * 要啟用，必須「同時」滿足（缺一不可）：
 *   1. benefit.crawler.apify.enabled=true   （環境變數 BENEFIT_CRAWLER_APIFY_ENABLED=true）
 *   2. 設定 APIFY_TOKEN 與 APIFY_FACEBOOK_ACTOR
 *   3. BenefitPostSource 的 benefit.crawler.source=http （見 BenefitPostSource）
 * 沒開啟時 fetchPosts 會直接丟 IllegalStateException，不會送出任何請求。
 */
@Component
public class ApifyFacebookClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final JsonMapper objectMapper;
    private final String actor;
    private final String token;
    private final int timeoutSeconds;
    private final boolean enabled;

    public ApifyFacebookClient(
            JsonMapper objectMapper,
            @Value("${benefit.crawler.apify.actor:}") String actor,
            @Value("${benefit.crawler.apify.token:}") String token,
            @Value("${benefit.crawler.apify.timeout-seconds:300}") int timeoutSeconds,
            @Value("${benefit.crawler.apify.enabled:false}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.actor = actor == null ? "" : actor.trim();
        this.token = token == null ? "" : token.trim();
        this.timeoutSeconds = Math.max(30, Math.min(timeoutSeconds, 300));
        this.enabled = enabled;
    }

    public List<JsonNode> fetchPosts(List<String> facebookUrls, int maxPosts)
            throws IOException, InterruptedException {
        // 總開關：預設 false，確保測試與日常開發不會意外消耗額度
        if (!enabled) {
            throw new IllegalStateException(
                    "Apify 實際 HTTP 請求目前停用。若確定要消耗額度，請設定 "
                            + "benefit.crawler.apify.enabled=true（環境變數 BENEFIT_CRAWLER_APIFY_ENABLED=true）");
        }
        if (token.isBlank()) {
            throw new IllegalStateException(
                    "尚未設定 benefit.crawler.apify.token（環境變數APIFY_TOKEN）");
        }
        if (actor.isBlank()) {
            throw new IllegalStateException(
                    "尚未設定 benefit.crawler.apify.actor，請填入你在Apify使用的Facebook Actor ID");
        }
        if (facebookUrls == null || facebookUrls.isEmpty()) {
            throw new IllegalArgumentException("至少要設定一個Facebook粉專網址");
        }

        var input = objectMapper.createObjectNode();
        var startUrls = input.putArray("startUrls");
        for (String facebookUrl : facebookUrls) {
            if (facebookUrl == null || facebookUrl.isBlank()) continue;
            var entry = startUrls.addObject();
            entry.put("url", facebookUrl.trim());
        }
        if (startUrls.isEmpty()) {
            throw new IllegalArgumentException("沒有有效的 Facebook 粉專網址");
        }
        input.put("resultsLimit", Math.max(1, Math.min(maxPosts, 500)));
        // 專題只需要特典圖片，不下載或解析影片字幕。
        input.put("includeVideoTranscript", false);

        String encodedActor = URLEncoder.encode(actor, StandardCharsets.UTF_8).replace("%7E", "~");
        // token 改放 Authorization header（Apify 支援 Bearer），不再放網址，避免出現在 log／例外訊息
        URI uri = URI.create("https://api.apify.com/v2/acts/" + encodedActor
                + "/run-sync-get-dataset-items?timeout=" + timeoutSeconds + "&format=json&clean=true");

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds + 30L))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(input), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = response.body() == null ? "" : response.body();
            if (body.length() > 1200) body = body.substring(0, 1200);
            throw new IOException("Apify Facebook Actor 呼叫失敗，HTTP "
                    + response.statusCode() + "：" + body);
        }

        JsonNode root = objectMapper.readTree(response.body());
        List<JsonNode> items = new ArrayList<>();
        if (root != null && root.isArray()) {
            root.forEach(items::add);
        } else if (root != null && root.path("items").isArray()) {
            root.path("items").forEach(items::add);
        }
        return items;
    }
}
