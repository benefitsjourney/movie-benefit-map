package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.dto.BenefitUpdateRequest;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.repository.BenefitRepository;

@ExtendWith(MockitoExtension.class)
class BenefitAdminServiceTest {

    @Mock
    private BenefitRepository benefitRepository;

    @InjectMocks
    private BenefitAdminService benefitAdminService;

    private BenefitBean benefit;

    @BeforeEach
    void setUp() {
        benefit = new BenefitBean();
        benefit.setBenefitId(1);
        benefit.setBenefitName("測試特典");
        benefit.setIsMemberOnly(false);
        benefit.setBenefitType(BenefitType.REGULAR.name());
        benefit.setStatus(BenefitStatus.APPROVED.name());
        benefit.setStartDate(LocalDateTime.now().minusDays(1));
        benefit.setEndDate(LocalDateTime.now().plusDays(5));
        benefit.setSourceUrl("https://example.com/original");

        lenient().when(benefitRepository.findById(1)).thenReturn(Optional.of(benefit));
        lenient().when(benefitRepository.save(any(BenefitBean.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("修改特典時只更新有提供的欄位")
    void updateOnlyProvidedFields() {
        BenefitUpdateRequest request = new BenefitUpdateRequest(
                "更新後特典名稱", true, null, null, null, null, null, null);

        benefitAdminService.update(1, request);

        assertEquals("更新後特典名稱", benefit.getBenefitName());
        assertEquals(true, benefit.getIsMemberOnly());
        assertEquals("https://example.com/original", benefit.getSourceUrl());
        assertEquals("APPROVED", benefit.getStatus());
        verify(benefitRepository).save(benefit);
    }

    @Test
    @DisplayName("修改圖片時，官方圖片網址與publicId必須一起提供")
    void updateShouldRejectIncompleteImageFields() {
        BenefitUpdateRequest request = new BenefitUpdateRequest(
                null, null, null, null, null, "https://example.com/image.jpg", null, null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> benefitAdminService.update(1, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(benefitRepository);
    }

    @Test
    @DisplayName("特典名稱不可為空白")
    void updateShouldRejectBlankName() {
        BenefitUpdateRequest request = new BenefitUpdateRequest(
                "   ", null, null, null, null, null, null, null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> benefitAdminService.update(1, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(benefitRepository, never()).save(any(BenefitBean.class));
    }

    @Test
    @DisplayName("下架時間早於開始時間時拒絕修改")
    void updateShouldRejectEndDateBeforeStartDate() {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.minusDays(1);
        BenefitUpdateRequest request = new BenefitUpdateRequest(
                null, null, null, start, end, null, null, null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> benefitAdminService.update(1, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(benefitRepository, never()).save(any(BenefitBean.class));
    }

    @Test
    @DisplayName("修改不存在的特典應回傳404")
    void updateShouldReturnNotFound() {
        when(benefitRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> benefitAdminService.update(999,
                        new BenefitUpdateRequest("新名稱", null, null, null, null, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("找不到特典：999", exception.getReason());
    }

    @Test
    @DisplayName("下架特典應更新狀態為EXPIRED")
    void updateStatusToExpired() {
        benefitAdminService.updateStatus(1, BenefitStatus.EXPIRED);

        assertEquals("EXPIRED", benefit.getStatus());
        verify(benefitRepository).save(benefit);
    }

    @Test
    @DisplayName("狀態沒有改變時不可重複寫入")
    void updateStatusShouldNotSaveWhenUnchanged() {
        benefitAdminService.updateStatus(1, BenefitStatus.APPROVED);

        verify(benefitRepository, never()).save(any(BenefitBean.class));
    }

    @Test
    @DisplayName("結束日期已過的特典不可重新上架")
    void updateStatusShouldRejectExpiredBenefit() {
        benefit.setStatus(BenefitStatus.EXPIRED.name());
        benefit.setEndDate(LocalDateTime.now().minusDays(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> benefitAdminService.updateStatus(1, BenefitStatus.APPROVED));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(benefitRepository, never()).save(any(BenefitBean.class));
    }

    @Test
    @DisplayName("狀態不可為空")
    void updateStatusShouldRejectNullStatus() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> benefitAdminService.updateStatus(1, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(benefitRepository);
    }

    @Test
    @DisplayName("已有會員持有特典時不可刪除")
    void deleteShouldRejectBenefitWithOwners() {
        when(benefitRepository.countUseBenefits(1)).thenReturn(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> benefitAdminService.delete(1));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(benefitRepository, never()).deleteById(1);
        verify(benefitRepository, never()).deleteLinksByBenefit(1);
    }

    @Test
    @DisplayName("沒有會員持有時要依序刪除特典關聯資料")
    void deleteShouldRemoveRelatedRecordsBeforeBenefit() {
        when(benefitRepository.countUseBenefits(1)).thenReturn(0L);

        benefitAdminService.delete(1);

        InOrder order = inOrder(benefitRepository);
        order.verify(benefitRepository).deleteInventoryContributionsByBenefit(1);
        order.verify(benefitRepository).deleteInventoryReportsByBenefit(1);
        order.verify(benefitRepository).deleteLinksByBenefit(1);
        order.verify(benefitRepository).detachReportsFromBenefit(1);
        order.verify(benefitRepository).deleteById(1);
    }
}
