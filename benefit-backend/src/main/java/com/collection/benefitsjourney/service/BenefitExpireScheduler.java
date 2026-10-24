package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.repository.BenefitRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 特典自動下架排程。
 * 後端啟動完成時跑一次，之後每天00:05（台北時間）跑一次，關機期間該下架的會在下次啟動時一次補做完。
 * 有結束日、而且已經過了→已下架。
 * 送完為止（沒有結束日）、開始日已過20週→已下架。
 * 只會把上架中改成已下架，不會把手動下架的改回上架中。
 * 爬蟲排程BenefitCrawlerScheduler是另一個檔案，維持關閉。
 */
@Slf4j
@Component
public class BenefitExpireScheduler {

    /** 送完為止的特典，開始日起幾週後自動下架 */
    public static final int UNTIL_OUT_WEEKS = 20;

    private final BenefitRepository benefitRepository;

    public BenefitExpireScheduler(BenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    /** 後端啟動完成時先檢查一次 */
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        run("後端啟動");
    }

    /** 每天台北時間00:05檢查一次 */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Taipei")
    public void runDaily() {
        run("每日排程");
    }

    private void run(String trigger) {
        try {
            int count = expire(LocalDateTime.now());
            log.info("{}：特典自動下架檢查完成，本次下架 {} 筆", trigger, count);
        } catch (Exception e) {
            log.error("{}：特典自動下架檢查失敗", trigger, e);
        }
    }

    /**
     * 把該下架的上架中特典改成已下架。
     * @param now 現在時間（測試時可以傳指定時間）
     * @return 這次下架的筆數
     */
    public int expire(LocalDateTime now) {
        String approved = BenefitStatus.APPROVED.name();
        List<BenefitBean> ended = benefitRepository.findByStatusAndEndDateBefore(approved, now);
        List<BenefitBean> untilOut = benefitRepository.findByStatusAndEndDateIsNullAndStartDateBefore(approved,
                now.minusWeeks(UNTIL_OUT_WEEKS));
        int count = 0;
        for (List<BenefitBean> list : List.of(ended, untilOut)) {
            for (BenefitBean b : list) {
                b.setStatus(BenefitStatus.EXPIRED.name());
                benefitRepository.save(b);
                count++;
            }
        }
        return count;
    }
}
