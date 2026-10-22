package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.collection.benefitsjourney.service.BenefitCrawlerImportService.ImportSummary;

import lombok.extern.slf4j.Slf4j;

/**
 * 特典爬蟲的定時執行，預設關閉。
 * 要開的時候加benefit.crawler.schedule.enabled=true（用yml格式）
 * 其他設定（都可省略）：
 * benefit.crawler.schedule.cron=0 0 10,22 * * *   （每天 10:00、22:00，台北時間）
 * benefit.crawler.schedule.limit=15               （每次只抓最新 15 篇；重複的貼文會自動略過）
 * benefit.crawler.pages=https://www.facebook.com/vieshow,https://www.facebook.com/其他影城
 * 每抓完一個網址都會寫一筆爬蟲紀錄（類型：特典），跟後台手動按的一樣。
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "benefit.crawler.schedule.enabled", havingValue = "true")
public class BenefitCrawlerScheduler {

    private final BenefitApifyService benefitApifyService;
    private final BenefitCrawlerLogRecorder crawlerLogRecorder;

    @Value("${benefit.crawler.schedule.limit:15}")
    private int limit;

    @Value("${benefit.crawler.pages:https://www.facebook.com/vieshow}")
    private List<String> pages;

    public BenefitCrawlerScheduler(BenefitApifyService benefitApifyService,
                                   BenefitCrawlerLogRecorder crawlerLogRecorder) {
        this.benefitApifyService = benefitApifyService;
        this.crawlerLogRecorder = crawlerLogRecorder;
    }

    @Scheduled(cron = "${benefit.crawler.schedule.cron:0 0 10,22 * * *}", zone = "Asia/Taipei")
    public void run() {
        for (String page : pages) {
            String url = page.trim();
            String source = "排程Apify同步（" + url + "）";
            LocalDateTime start = LocalDateTime.now();
            try {
                ImportSummary s = benefitApifyService.importFromPage(url, limit, true);
                crawlerLogRecorder.recordImport(source, s, start);
                log.info("排程爬蟲完成 {}：新增 {}（自動上架 {}）、重複 {}、略過 {}",
                        url, s.created(), s.autoApproved(), s.duplicated(), s.ignored());
            } catch (Exception e) {
                // 一間失敗不要影響其他影城，也不要讓排程停掉
                crawlerLogRecorder.recordFailure(source, e, start);
                log.error("排程爬蟲失敗：{}", url, e);
            }
        }
    }
}
