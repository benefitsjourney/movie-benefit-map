package com.collection.benefitsjourney.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.InventoryReportBean;
import com.collection.benefitsjourney.dto.InventoryReportResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;
import com.collection.benefitsjourney.enums.ContributionType;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.InventoryReportRepository;
import com.collection.benefitsjourney.repository.UserContributionRepository;

import lombok.RequiredArgsConstructor;

/** 管理員端：查詢、刪除庫存回報 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryReportAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryReportRepository inventoryReportRepository;
    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;
    private final UserContributionRepository userContributionRepository;

    /** 查詢全部庫存回報（新到舊），頁碼從1開始 */
    public PageResponse<InventoryReportResponse> findAllReports(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<InventoryReportResponse> result = inventoryReportRepository
                .findAllReports(PageRequest.of(safePage - 1, safeSize))
                .map(InventoryReportResponse::from);

        return PageResponse.from(result);
    }

    /** 刪除指定庫存回報，並同步更新貢獻紀錄及庫存狀態 */
    @Transactional
    public void deleteReport(Integer reportId) {
        InventoryReportBean report = inventoryReportRepository.findById(reportId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到庫存回報：" + reportId));

        Integer cinemaMovieBenefitId =
                report.getCinemaMovieBenefit().getCinemaMovieBenefitId();

        // 1. 刪除該筆回報對應的貢獻紀錄
        userContributionRepository.deleteByContributionTypeAndReferenceId(
                ContributionType.INVENTORY_REPORT.name(), reportId);

        // 2. 刪除庫存回報
        inventoryReportRepository.delete(report);
        inventoryReportRepository.flush();

        // 3. 重新計算該影城特典目前應顯示的庫存狀態
        var link = cinemaMovieBenefitRepository.findById(cinemaMovieBenefitId)
                .orElseThrow(() -> ExceptionUtils.notFound(
                        "找不到影城特典：" + cinemaMovieBenefitId));

        InventoryReportBean latest = inventoryReportRepository
                .findFirstByCinemaMovieBenefit_CinemaMovieBenefitIdOrderByReportedAtDescReportIdDesc(
                        cinemaMovieBenefitId)
                .orElse(null);

        if (latest == null) {
            link.setBenefitInventoryStatus(BenefitInventoryStatus.UNKNOWN.getCode());
        } else {
            link.setBenefitInventoryStatus(latest.getStatus());
        }

        cinemaMovieBenefitRepository.save(link);
    }
}