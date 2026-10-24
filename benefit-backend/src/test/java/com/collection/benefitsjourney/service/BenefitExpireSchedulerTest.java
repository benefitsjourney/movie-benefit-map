package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.repository.BenefitRepository;

@ExtendWith(MockitoExtension.class)
class BenefitExpireSchedulerTest {

    @Mock BenefitRepository benefitRepo;
    @InjectMocks BenefitExpireScheduler scheduler;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 0, 5);

    private BenefitBean approved(int id) {
        BenefitBean b = new BenefitBean();
        b.setBenefitId(id);
        b.setStatus("APPROVED");
        return b;
    }

    @Test
    @DisplayName("結束日已過、送完為止開始日已過20週：都改成已下架")
    void expireBoth() {
        BenefitBean ended = approved(1);
        BenefitBean untilOut = approved(2);
        when(benefitRepo.findByStatusAndEndDateBefore("APPROVED", NOW)).thenReturn(List.of(ended));
        when(benefitRepo.findByStatusAndEndDateIsNullAndStartDateBefore("APPROVED", NOW.minusWeeks(20)))
                .thenReturn(List.of(untilOut));

        int count = scheduler.expire(NOW);

        assertEquals(2, count);
        assertEquals("EXPIRED", ended.getStatus());
        assertEquals("EXPIRED", untilOut.getStatus());
        verify(benefitRepo, times(2)).save(any());
    }

    @Test
    @DisplayName("沒有該下架的：不改任何資料")
    void nothingToExpire() {
        when(benefitRepo.findByStatusAndEndDateBefore("APPROVED", NOW)).thenReturn(List.of());
        when(benefitRepo.findByStatusAndEndDateIsNullAndStartDateBefore("APPROVED", NOW.minusWeeks(20)))
                .thenReturn(List.of());

        assertEquals(0, scheduler.expire(NOW));
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("送完為止的下架時間是開始日起20週")
    void untilOutWeeks() {
        assertEquals(20, BenefitExpireScheduler.UNTIL_OUT_WEEKS);
    }
}
