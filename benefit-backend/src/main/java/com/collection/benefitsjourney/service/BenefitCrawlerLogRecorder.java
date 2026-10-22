package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.ImportSummary;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.RematchSummary;

/**
 * 把特典爬蟲的執行結果寫進爬蟲紀錄（組員的 crawler_log，後台「爬蟲紀錄」頁選「特典」就看得到）。
 * 後台按鈕（BenefitCrawlerController）和排程（BenefitCrawlerScheduler）都呼叫這裡，
 * 所以不管是誰觸發的都會留下紀錄（做法同組員的影城／電影同步）。
 * 寫入失敗只會記 log，不會影響爬蟲本身的結果（CrawlerLogService.recordRun 本身就有處理）。
 */
@Service
public class BenefitCrawlerLogRecorder {

    private final CrawlerLogService crawlerLogService;

    public BenefitCrawlerLogRecorder(CrawlerLogService crawlerLogService) {
        this.crawlerLogService = crawlerLogService;
    }

    /**
     * Apify同步／JSON匯入完成：
     * 抓取＝這次處理的貼文筆數（新增＋重複＋非特典）、新增＝新增的特典與待審單、
     * 略過＝重複＋非特典、待審核＝新增裡沒能直接上架的
     */
    public void recordImport(String source, ImportSummary s, LocalDateTime start) {
        int pending = Math.max(s.created() - s.autoApproved(), 0);
        String message = "%s：新增 %d 筆（直接上架 %d、待審核 %d）、重複略過 %d 筆、非特典 %d 筆。"
                .formatted(source, s.created(), s.autoApproved(), pending, s.duplicated(), s.ignored());
        crawlerLogService.recordRun(
                CrawlerType.BENEFIT, CrawlerStatus.SUCCESS,
                s.created() + s.duplicated() + s.ignored(),
                s.created(),
                0,
                s.duplicated() + s.ignored(),
                pending,
                message, start, LocalDateTime.now());
    }

    /**
     * 重新比對完成：
     * 抓取＝檢查的待審單、新增＝直接上架成正式特典的、更新＝找到電影但資料還不齊（綁好電影仍待審）、
     * 略過＝還是找不到電影的、待審核＝比對完仍留在待審核的
     */
    public void recordRematch(RematchSummary s, LocalDateTime start) {
        String message = "重新比對：檢查 %d 筆、找到電影 %d 筆、直接上架 %d 筆、仍待審核 %d 筆。"
                .formatted(s.checked(), s.movieMatched(), s.approved(), s.stillPending());
        crawlerLogService.recordRun(
                CrawlerType.BENEFIT, CrawlerStatus.SUCCESS,
                s.checked(),
                s.approved(),
                Math.max(s.movieMatched() - s.approved(), 0),
                Math.max(s.checked() - s.movieMatched(), 0),
                s.stillPending(),
                message, start, LocalDateTime.now());
    }

    /** 執行失敗：筆數都記0，訊息寫失敗原因 */
    public void recordFailure(String source, Throwable error, LocalDateTime start) {
        crawlerLogService.recordRun(
                CrawlerType.BENEFIT, CrawlerStatus.FAILURE,
                0, 0, 0, 0, 0,
                source + "失敗：" + CrawlerLogService.failureMessage(error),
                start, LocalDateTime.now());
    }
}
