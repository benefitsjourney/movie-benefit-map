package com.collection.benefitsjourney.service;

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
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "benefit.crawler.schedule.enabled", havingValue = "true")
public class BenefitCrawlerScheduler {

    private final BenefitApifyService benefitApifyService;

    @Value("${benefit.crawler.schedule.limit:15}")
    private int limit;

    @Value("${benefit.crawler.pages:https://www.facebook.com/vieshow}")
    private List<String> pages;

    public BenefitCrawlerScheduler(BenefitApifyService benefitApifyService) {
        this.benefitApifyService = benefitApifyService;
    }

    @Scheduled(cron = "${benefit.crawler.schedule.cron:0 0 10,22 * * *}", zone = "Asia/Taipei")
    public void run() {
        for (String page : pages) {
            String url = page.trim();
            try {
                ImportSummary s = benefitApifyService.importFromPage(url, limit, true);
                log.info("排程爬蟲完成 {}：新增 {}（自動上架 {}）、重複 {}、略過 {}",
                        url, s.created(), s.autoApproved(), s.duplicated(), s.ignored());
            } catch (Exception e) {
                // 一間失敗不要影響其他影城，也不要讓排程停掉
                log.error("排程爬蟲失敗：{}", url, e);
            }
        }
    }
}