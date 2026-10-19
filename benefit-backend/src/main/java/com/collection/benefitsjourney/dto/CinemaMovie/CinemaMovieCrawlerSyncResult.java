package com.collection.benefitsjourney.dto.CinemaMovie;

import java.util.List;

/** 單次爬蟲成功後的結果統計。 */
public record CinemaMovieCrawlerSyncResult<T>(
        int fetchedCount,
        int createdCount,
        int skippedCount,
        int updatedCount,
        int reviewCount,
        long elapsedSeconds,
        String message,
        List<T> items) {

    public static <T> CinemaMovieCrawlerSyncResult<T> of(
            int fetchedCount,
            int createdCount,
            int skippedCount,
            int updatedCount,
            int reviewCount,
            long elapsedSeconds,
            List<T> items) {
        String message = "本次抓到 %d 筆、新增 %d 筆、略過 %d 筆、更新 %d 筆、納入審核 %d 筆，耗時 %d 秒。"
                .formatted(fetchedCount, createdCount, skippedCount, updatedCount, reviewCount, elapsedSeconds);
        return new CinemaMovieCrawlerSyncResult<>(
                fetchedCount,
                createdCount,
                skippedCount,
                updatedCount,
                reviewCount,
                elapsedSeconds,
                message,
                List.copyOf(items));
    }
}
