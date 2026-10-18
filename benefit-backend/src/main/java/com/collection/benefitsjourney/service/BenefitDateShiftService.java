package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;

/**
 * 【測試／展示用】把所有特典的開始日、結束日一起平移幾天，再按照新的結束日重新判斷上架或下架。
 * 只改日期與狀態，特典ID、電影、影城、中介表都不變，所以不影響各表之間的關聯。
 * 可以重複呼叫（例如部署前、發表前各一次），days=0則只重新判斷狀態、不動日期。
 */
@Service
public class BenefitDateShiftService {

    /** 一次最多平移一年，避免打錯數字日期變得很瞎 */
    public static final int MAX_DAYS = 365;

    private final BenefitRepository benefitRepository;

    public BenefitDateShiftService(BenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    /**
     * total＝特典總數、shifted＝有被平移日期的筆數
     * approvedNow／expiredNow＝處理完之後上架中／已下架的筆數、statusChanged＝狀態有改變的筆數
     */
    public record ShiftSummary(int total, int shifted, int approvedNow, int expiredNow, int statusChanged) {
    }

    @Transactional
    public ShiftSummary shiftDates(int days) {
        if (days < -MAX_DAYS || days > MAX_DAYS) {
            throw ExceptionUtils.badRequest("days要介於 -" + MAX_DAYS + " 到 " + MAX_DAYS + " 之間");
        }
        LocalDateTime now = LocalDateTime.now();
        List<BenefitBean> all = benefitRepository.findAll();

        int shifted = 0;
        int approved = 0;
        int expired = 0;
        int changed = 0;
        for (BenefitBean b : all) {
            if (days != 0 && (b.getStartDate() != null || b.getEndDate() != null)) {
                if (b.getStartDate() != null) {
                    b.setStartDate(b.getStartDate().plusDays(days));
                }
                if (b.getEndDate() != null) {
                    b.setEndDate(b.getEndDate().plusDays(days));
                }
                shifted++;
            }
            // 有結束日才重新判斷，「送完為止」沒有結束日，維持原狀態
            if (b.getEndDate() != null) {
                String status = (b.getEndDate().isBefore(now) ? BenefitStatus.EXPIRED : BenefitStatus.APPROVED).name();
                if (!status.equals(b.getStatus())) {
                    b.setStatus(status);
                    changed++;
                }
            }
            if (BenefitStatus.APPROVED.name().equals(b.getStatus())) {
                approved++;
            } else if (BenefitStatus.EXPIRED.name().equals(b.getStatus())) {
                expired++;
            }
            benefitRepository.save(b);
        }
        return new ShiftSummary(all.size(), shifted, approved, expired, changed);
    }
}