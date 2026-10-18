package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.service.BenefitDateShiftService.ShiftSummary;

@ExtendWith(MockitoExtension.class)
class BenefitDateShiftServiceTest {

    @Mock BenefitRepository benefitRepo;
    @InjectMocks BenefitDateShiftService service;

    private BenefitBean benefit(LocalDateTime start, LocalDateTime end, String status) {
        BenefitBean b = new BenefitBean();
        b.setStartDate(start);
        b.setEndDate(end);
        b.setStatus(status);
        return b;
    }

    @Test
    @DisplayName("平移後結束日仍在過去：日期照樣平移，狀態維持已下架")
    void stillExpiredAfterShift() {
        BenefitBean b = benefit(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2020, 1, 7, 23, 59, 59), "EXPIRED");
        when(benefitRepo.findAll()).thenReturn(List.of(b));

        ShiftSummary s = service.shiftDates(365);

        assertEquals(LocalDateTime.of(2020, 12, 31, 0, 0), b.getStartDate());
        assertEquals("EXPIRED", b.getStatus());
        assertEquals(new ShiftSummary(1, 1, 0, 1, 0), s);
    }

    @Test
    @DisplayName("結束日平移後在未來：改回上架（APPROVED）")
    void shiftedIntoFuture() {
        LocalDateTime end = LocalDateTime.now().minusDays(3);
        BenefitBean b = benefit(end.minusDays(6), end, "EXPIRED");
        when(benefitRepo.findAll()).thenReturn(List.of(b));

        ShiftSummary s = service.shiftDates(14);

        assertEquals(end.plusDays(14), b.getEndDate());
        assertEquals(end.minusDays(6).plusDays(14), b.getStartDate());
        assertEquals("APPROVED", b.getStatus());
        assertEquals(new ShiftSummary(1, 1, 1, 0, 1), s);
        verify(benefitRepo).save(b);
    }

    @Test
    @DisplayName("往前平移：原本上架的變成已下架")
    void shiftBackward() {
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        BenefitBean b = benefit(end.minusDays(6), end, "APPROVED");
        when(benefitRepo.findAll()).thenReturn(List.of(b));

        ShiftSummary s = service.shiftDates(-10);

        assertEquals("EXPIRED", b.getStatus());
        assertEquals(new ShiftSummary(1, 1, 0, 1, 1), s);
    }

    @Test
    @DisplayName("沒有結束日（送完為止）：只挪開始日，狀態維持原樣")
    void noEndDateKeepsStatus() {
        BenefitBean b = benefit(LocalDateTime.of(2026, 10, 1, 0, 0), null, "APPROVED");
        when(benefitRepo.findAll()).thenReturn(List.of(b));

        ShiftSummary s = service.shiftDates(7);

        assertEquals(LocalDateTime.of(2026, 10, 8, 0, 0), b.getStartDate());
        assertEquals("APPROVED", b.getStatus());
        assertEquals(new ShiftSummary(1, 1, 1, 0, 0), s);
    }

    @Test
    @DisplayName("days=0：不動日期，只依結束日重新判斷狀態")
    void zeroOnlyRecomputesStatus() {
        LocalDateTime end = LocalDateTime.now().minusDays(1);
        BenefitBean b = benefit(end.minusDays(6), end, "APPROVED");
        when(benefitRepo.findAll()).thenReturn(List.of(b));

        ShiftSummary s = service.shiftDates(0);

        assertEquals(end, b.getEndDate());
        assertEquals("EXPIRED", b.getStatus());
        assertEquals(new ShiftSummary(1, 0, 0, 1, 1), s);
    }

    @Test
    @DisplayName("天數超出＋－365：回400，不動任何資料")
    void outOfRange() {
        assertThrows(ResponseStatusException.class, () -> service.shiftDates(366));
        assertThrows(ResponseStatusException.class, () -> service.shiftDates(-366));
        verify(benefitRepo, never()).save(any());
    }
}