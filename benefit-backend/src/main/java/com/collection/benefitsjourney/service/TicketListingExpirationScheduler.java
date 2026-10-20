package com.collection.benefitsjourney.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketListingExpirationScheduler {
    private final TicketListingService ticketListingService;

    // Spring Boot 啟動完成後，立即檢查過期票券一次。
    @EventListener(ApplicationReadyEvent.class)
    public void runExpirationCheckOnStartup() {
        checkExpiredTicketListings("Spring Boot 啟動");
    }

    // 每天台灣時間凌晨 00:05 檢查過期票券。
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Taipei")
    public void runExpirationCheckDaily() {
        checkExpiredTicketListings("每日排程");
    }

    // 執行過期票券處理。
    private void checkExpiredTicketListings(String trigger) {
        try {
            int count = ticketListingService.markExpiredListingsAsDeleted();

            log.info(
                    "{}：過期票券檢查完成，本次更新 {} 筆票券。",
                    trigger, count);
        } catch (Exception e) {
            log.error("{}：過期票券檢查失敗。", trigger, e);
        }
    }
}
