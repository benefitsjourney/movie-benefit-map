package com.collection.benefitsjourney.enums;

/** 爬蟲執行狀態 */
public enum CrawlerStatus {
    /** 成功 */
    SUCCESS,

    /** 失敗 */
    FAILURE,

    /** 部分成功／失敗 */
    PARTIAL;

    /** 爬蟲有跑完時，略過或寫入失敗視為部分成功。 */
    public static CrawlerStatus completed(int skippedCount, int failedCount) {
        return skippedCount > 0 || failedCount > 0 ? PARTIAL : SUCCESS;
    }
}
