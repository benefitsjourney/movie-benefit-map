package com.collection.benefitsjourney.dto.CinemaMovie;

import java.util.List;

/** 單次爬蟲成功後的結果。清單放在 items，message 只供後端 log。 */
public record CinemaMovieCrawlerSyncResult<T>(
        int fetchedCount,
        int createdCount,
        int skippedCount,
        int updatedCount,
        int reviewCount,
        long elapsedSeconds,
        List<T> items) {

    public static <T> CinemaMovieCrawlerSyncResult<T> of(
            int fetchedCount,
            int createdCount,
            int skippedCount,
            int updatedCount,
            int reviewCount,
            long elapsedSeconds,
            List<T> items) {
        return new CinemaMovieCrawlerSyncResult<>(
                fetchedCount,
                createdCount,
                skippedCount,
                updatedCount,
                reviewCount,
                elapsedSeconds,
                List.copyOf(items));
    }

    /** 後端 log 用，不是 API 欄位。 */
    public String message() {
        return "本次抓到 %d 筆、新增 %d 筆、略過 %d 筆、更新 %d 筆、納入審核 %d 筆，耗時 %d 秒。"
                .formatted(fetchedCount, createdCount, skippedCount, updatedCount, reviewCount, elapsedSeconds);
    }
}
