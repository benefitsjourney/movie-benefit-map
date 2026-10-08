package com.collection.benefitsjourney.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitReportBean;
import com.collection.benefitsjourney.dto.BenefitReportAdminResponse;
import com.collection.benefitsjourney.dto.BenefitReportRejectRequest;
import com.collection.benefitsjourney.dto.BenefitReportUpdateRequest;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitReportStatus;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitReportRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitReportAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BenefitReportRepository benefitReportRepository;
    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;

    /** 依狀態查詢特典回報（先送先審），頁碼從1開始 */
    public PageResponse<BenefitReportAdminResponse> findByStatus(
            BenefitReportStatus status, int page, int size) {

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<BenefitReportAdminResponse> result = benefitReportRepository
                .findByStatusOrderByCreatedAtAsc(
                        status.name(),
                        PageRequest.of(safePage - 1, safeSize))
                .map(BenefitReportAdminResponse::from);

        return PageResponse.from(result);
    }

    /** 查詢單一特典回報（不限狀態，管理員也可查已處理的） */
    public BenefitReportAdminResponse findById(Integer id) {
        return BenefitReportAdminResponse.from(getOrThrow(id));
    }

    /** 修改待審核的特典回報，只更新有帶的欄位 */
    @Transactional
    public BenefitReportAdminResponse update(Integer id, BenefitReportUpdateRequest request) {
        if (request == null) {
            throw ExceptionUtils.badRequest("請求內容不可為空");
        }

        BenefitReportBean report = getPendingOrThrow(id);

        if (request.benefitName() != null && request.benefitName().isBlank()) {
            throw ExceptionUtils.badRequest("特典名稱不可為空");
        }

        if (request.cinemaId() != null) {
            if (!cinemaRepository.existsById(request.cinemaId())) {
                throw ExceptionUtils.badRequest("找不到影城ID：" + request.cinemaId());
            }
            report.setCinemaId(request.cinemaId());
        }

        if (request.movieId() != null) {
            if (!movieRepository.existsById(request.movieId())) {
                throw ExceptionUtils.badRequest("找不到電影ID：" + request.movieId());
            }
            report.setMovieId(request.movieId());
        }

        if (request.benefitName() != null) {
            report.setBenefitName(request.benefitName());
        }
        if (request.customCinemaName() != null) {
            report.setCustomCinemaName(request.customCinemaName());
        }
        if (request.customMovieName() != null) {
            report.setCustomMovieName(request.customMovieName());
        }
        if (request.isMemberOnly() != null) {
            report.setIsMemberOnly(request.isMemberOnly());
        }
        if (request.benefitType() != null) {
            report.setBenefitType(request.benefitType().name());
        }
        if (request.startDate() != null) {
            report.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            report.setEndDate(request.endDate());
        }
        if (request.officialImageUrl() != null) {
            report.setOfficialImageUrl(request.officialImageUrl());
        }
        if (request.sourceUrl() != null) {
            report.setSourceUrl(request.sourceUrl());
        }
        if (request.reviewNote() != null) {
            report.setReviewNote(request.reviewNote());
        }

        // 合併後再檢查，避免只改其中一個日期造成開始日晚於結束日
        if (report.getStartDate() != null
                && report.getEndDate() != null
                && report.getEndDate().isBefore(report.getStartDate())) {
            throw ExceptionUtils.badRequest("下架時間不可早於開放領取時間");
        }

        return BenefitReportAdminResponse.from(benefitReportRepository.save(report));
    }

    /** 駁回待審核的特典回報，reviewNote有帶才覆蓋 */
    @Transactional
    public BenefitReportAdminResponse reject(Integer id, BenefitReportRejectRequest request) {
        BenefitReportBean report = getPendingOrThrow(id);

        report.setStatus(BenefitReportStatus.REJECTED.name());

        if (request != null
                && request.reviewNote() != null
                && !request.reviewNote().isBlank()) {
            report.setReviewNote(request.reviewNote());
        }

        return BenefitReportAdminResponse.from(benefitReportRepository.save(report));
    }

    private BenefitReportBean getOrThrow(Integer id) {
        return benefitReportRepository.findById(id)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到特典回報：" + id));
    }

    /** 取得待審核的回報；已處理過的回傳409。之後approve也可共用 */
    private BenefitReportBean getPendingOrThrow(Integer id) {
        BenefitReportBean report = getOrThrow(id);
        if (!BenefitReportStatus.PENDING.name().equals(report.getStatus())) {
            throw ExceptionUtils.conflict("該回報已處理，無法再修改或駁回");
        }
        return report;
    }
}
