package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.BenefitReportBean;
import com.collection.benefitsjourney.repository.BenefitReportRepository;
import com.collection.benefitsjourney.repository.BenefitRepository;

/** Transaction/Rollback測試 */
@SpringBootTest
@DisplayName("BenefitAdminService測試")
class BenefitAdminServiceTransactionTest {

    @Autowired
    private BenefitAdminService benefitAdminService;

    @Autowired
    private BenefitReportRepository benefitReportRepository;

    @MockitoSpyBean
    private BenefitRepository benefitRepository;

    private Integer benefitId;
    private Integer reportId;

    @AfterEach
    void cleanUp() {
        reset(benefitRepository);

        if (reportId != null) {
            benefitReportRepository.deleteById(reportId);
        }
        if (benefitId != null) {
            benefitRepository.deleteById(benefitId);
        }
    }

    @Test
    @DisplayName("刪除成功時要提交變更，並解除回報與特典的關聯")
    void deleteShouldCommitAndDetachBenefitReport() {
        createTestData();

        benefitAdminService.delete(benefitId);

        assertFalse(benefitRepository.existsById(benefitId),
                "特典成功刪除");

        BenefitReportBean report = benefitReportRepository.findById(reportId)
                .orElseThrow();
        assertNull(report.getBenefit(),
                "歷史回報要保留，與正式特典的關聯解除");
    }

    @Test
    @DisplayName("刪除過程發生錯誤時要Rollback先前的變更")
    void deleteShouldRollbackEarlierChangesWhenLaterStepFails() {
        createTestData();

        // 模擬刪除特典時發生錯誤，確認先前解除回報關聯的操作也會Rollback
        doThrow(new IllegalStateException("測試用：模擬刪除特典時發生錯誤"))
                .when(benefitRepository).deleteById(benefitId);

        assertThrows(IllegalStateException.class,
                () -> benefitAdminService.delete(benefitId));

        BenefitBean benefit = benefitRepository.findById(benefitId).orElse(null);
        assertNotNull(benefit, "交易失敗後，特典仍存在");

        BenefitReportBean report = benefitReportRepository.findById(reportId)
                .orElseThrow();
        assertNotNull(report.getBenefit(),
                "交易失敗後，回報與特典的關聯恢復");
        assertEquals(benefitId, report.getBenefit().getBenefitId(),
                "回報要重新關聯到原本的特典");
    }

    private void createTestData() {
        String unique = "tx-test-" + UUID.randomUUID();

        BenefitBean benefit = new BenefitBean();
        benefit.setBenefitName(unique);
        benefit.setStatus("APPROVED");
        benefit = benefitRepository.saveAndFlush(benefit);
        benefitId = benefit.getBenefitId();

        BenefitReportBean report = new BenefitReportBean();
        report.setBenefit(benefit);
        report.setBenefitName(unique);
        report.setStatus("PENDING");
        report = benefitReportRepository.saveAndFlush(report);
        reportId = report.getBenefitReportId();
    }
}
