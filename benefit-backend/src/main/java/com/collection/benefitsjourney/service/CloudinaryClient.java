package com.collection.benefitsjourney.service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 把Facebook貼文（或網路上）的照片轉存到Cloudinary。
 * 因為Facebook的圖片網址有效期很短（幾天），所以要存一份在自己的Cloudinary。
 *
 * 做法：後端先把圖片下載下來，再用「簽名上傳」傳給Cloudinary（不用設定upload preset）。
 * 設定（金鑰勿寫在程式內，要用環境變數）：
 *   CLOUDINARY_CLOUD_NAME、CLOUDINARY_API_KEY、CLOUDINARY_API_SECRET，（可選）CLOUDINARY_FOLDER，預設benefits。
 * 沒設定、或單張上傳失敗，都只會寫log並回傳空值，不會讓整批匯入失敗。
 */
@Slf4j
@Service
public class CloudinaryClient {

    /** 轉存成功後要存進資料庫的兩個值：網址（official_image_url）與publicId（public_id） */
    public record UploadedImage(String url, String publicId) {
    }

    /** Cloudinary免費方案單張圖片上限10MB */
    static final int MAX_BYTES = 10 * 1024 * 1024;

    @Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudName;

    @Value("${cloudinary.api-key:${CLOUDINARY_API_KEY:}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:${CLOUDINARY_API_SECRET:}}")
    private String apiSecret;

    /** 圖片放在Cloudinary的哪個資料夾（空字串＝不指定） */
    @Value("${cloudinary.folder:${CLOUDINARY_FOLDER:benefits}}")
    private String folder;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isConfigured() {
        return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
    }

    /** 下載imageUrl並上傳到Cloudinary。沒設定、下載失敗、上傳失敗都回Optional.empty()（原因寫在log） */
    public Optional<UploadedImage> uploadFromUrl(String imageUrl) {
        if (!isConfigured()) {
            log.warn("尚未設定CLOUDINARY_CLOUD_NAME/CLOUDINARY_API_KEY/CLOUDINARY_API_SECRET，略過圖片轉存");
            return Optional.empty();
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            return Optional.empty();
        }
        byte[] bytes;
        String contentType;
        try {
            HttpResponse<byte[]> download = http.send(HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (download.statusCode() != 200) {
                log.warn("【下載圖片】失敗 HTTP {}（圖片網址可能已過期）：{}", download.statusCode(), hostOf(imageUrl));
                return Optional.empty();
            }
            bytes = download.body();
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                log.warn("【下載圖片】圖片大小不符（{} bytes），略過", bytes.length);
                return Optional.empty();
            }
            contentType = download.headers().firstValue("Content-Type").orElse("application/octet-stream");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("【下載圖片】連不到 {}：{}", hostOf(imageUrl), describe(e));
            return Optional.empty();
        }
        try {
            return upload(bytes, contentType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("【上傳Cloudinary】連不到 api.cloudinary.com：{}", describe(e));
            return Optional.empty();
        }
    }

    /**
     * 連線檢查（GET /crawler/benefits/cloudinary-check用，管理員權限）
     * 1. 金鑰有沒有設定　2. 連不連得到 Cloudinary、金鑰是否正確　3.（可選）能不能下載指定的圖片網址
     */
    public java.util.List<String> diagnose(String imageUrl) {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add((notBlank(cloudName) ? "✔" : "✘") + " CLOUDINARY_CLOUD_NAME " + (notBlank(cloudName) ? "已設定" : "未設定"));
        out.add((notBlank(apiKey) ? "✔" : "✘") + " CLOUDINARY_API_KEY " + (notBlank(apiKey) ? "已設定" : "未設定"));
        out.add((notBlank(apiSecret) ? "✔" : "✘") + " CLOUDINARY_API_SECRET " + (notBlank(apiSecret) ? "已設定" : "未設定"));
        if (isConfigured()) {
            try {
                String basic = java.util.Base64.getEncoder()
                        .encodeToString((apiKey.trim() + ":" + apiSecret.trim()).getBytes(StandardCharsets.UTF_8));
                HttpResponse<String> r = http.send(HttpRequest.newBuilder()
                        .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName.trim() + "/ping"))
                        .timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Basic " + basic)
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
                out.add(switch (r.statusCode()) {
                    case 200 -> "✔ 連得到Cloudinary，金鑰正確";
                    case 401 -> "✘ 連得到Cloudinary，但API Key/Secret不對（HTTP 401）";
                    case 404 -> "✘ 連得到Cloudinary，但cloud name不存在（HTTP 404）";
                    default -> "✘ Cloudinary回應HTTP " + r.statusCode() + "：" + abbreviate(r.body());
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                out.add("✘ 連不到api.cloudinary.com：" + describe(e));
            }
        }
        if (notBlank(imageUrl)) {
            try {
                HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder().uri(URI.create(imageUrl))
                        .timeout(Duration.ofSeconds(30)).header("User-Agent", "Mozilla/5.0")
                        .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                out.add(r.statusCode() == 200
                        ? "✔ 圖片可下載（" + r.body().length + " bytes）"
                        : "✘ 圖片下載失敗HTTP " + r.statusCode() + "（網址可能已過期）");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                out.add("✘ 連不到圖片主機 " + hostOf(imageUrl) + "：" + describe(e));
            }
        }
        return out;
    }

    /** ConnectException沒有訊息，連原因的類別一起列出 */
    static String describe(Throwable e) {
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName());
        if (e.getMessage() != null) {
            sb.append(" ").append(e.getMessage());
        }
        Throwable c = e.getCause();
        while (c != null && c != e) {
            sb.append(" ← ").append(c.getClass().getSimpleName());
            if (c.getMessage() != null) {
                sb.append(" ").append(c.getMessage());
            }
            e = c;
            c = c.getCause();
        }
        return sb.toString();
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return "(網址格式錯誤)";
        }
    }

    private Optional<UploadedImage> upload(byte[] bytes, String contentType) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = sign(folder, timestamp, apiSecret);
        String boundary = "----benefits" + System.nanoTime();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        field(body, boundary, "api_key", apiKey);
        field(body, boundary, "timestamp", String.valueOf(timestamp));
        if (notBlank(folder)) {
            field(body, boundary, "folder", folder);
        }
        field(body, boundary, "signature", signature);
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpResponse<String> response = http.send(HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName.trim() + "/image/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Cloudinary上傳失敗HTTP {}：{}", response.statusCode(), abbreviate(response.body()));
            return Optional.empty();
        }
        JsonNode json = mapper.readTree(response.body());
        String url = json.path("secure_url").asText(null);
        String publicId = json.path("public_id").asText(null);
        if (url == null || publicId == null) {
            log.warn("Cloudinary回應缺少secure_url/public_id");
            return Optional.empty();
        }
        return Optional.of(new UploadedImage(url, publicId));
    }

    /**
     * Cloudinary簽名規則：把要簽名的參數照字母排序，組成"folder=xxx&timestamp=123"，
     * 後面直接接API Secret（中間沒有符號），再做SHA-1，轉成小寫16進位。
     * （file、api_key不參與簽名）
     */
    static String sign(String folder, long timestamp, String secret) {
        String toSign = (notBlank(folder) ? "folder=" + folder + "&" : "") + "timestamp=" + timestamp;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((toSign + secret).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void field(ByteArrayOutputStream out, String boundary, String name, String value) throws java.io.IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String abbreviate(String s) {
        return s == null ? "" : (s.length() <= 200 ? s : s.substring(0, 200) + "…");
    }
}