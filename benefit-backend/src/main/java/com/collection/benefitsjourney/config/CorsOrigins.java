package com.collection.benefitsjourney.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CORS 與 WebSocket 共用的允許來源。
 * 常駐網域寫在這裡；會變的網址放環境變數 CORS_ALLOWED_ORIGINS（逗號分隔）。
 * 本機與區網用模式比對，換 port 或區網 IP 不必再改程式。
 */
@Component
public class CorsOrigins {

    private static final List<String> PERMANENT_ORIGINS = List.of(
            "https://benefitsjourney.github.io",
            "https://movie-benefit-map.onrender.com");

    private static final List<String> LOCAL_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*:*");

    private final List<String> allowedOrigins;
    private final List<String> allowedOriginPatterns;

    public CorsOrigins(
            @Value("${app.cors.origins:}") String extraOrigins,
            @Value("${app.cors.allow-local-patterns:true}") boolean allowLocalPatterns) {
        List<String> origins = new ArrayList<>(PERMANENT_ORIGINS);
        for (String part : extraOrigins.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                origins.add(trimmed);
            }
        }
        this.allowedOrigins = List.copyOf(origins);
        this.allowedOriginPatterns = allowLocalPatterns ? LOCAL_PATTERNS : List.of();
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    /** WebSocket 只吃 pattern；精確網域也一併放進去，沒有 * 時就是完全比對。 */
    public String[] websocketOriginPatterns() {
        List<String> combined = new ArrayList<>(allowedOrigins);
        combined.addAll(allowedOriginPatterns);
        return combined.toArray(String[]::new);
    }
}
