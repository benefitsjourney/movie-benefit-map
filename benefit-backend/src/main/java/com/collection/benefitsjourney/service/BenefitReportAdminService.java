package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.BenefitReportBean;
import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.UserContributionBean;
import com.collection.benefitsjourney.dto.BenefitReportAdminResponse;
import com.collection.benefitsjourney.dto.BenefitReportApproveRequest;
import com.collection.benefitsjourney.dto.BenefitReportRejectRequest;
import com.collection.benefitsjourney.dto.BenefitReportUpdateRequest;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitReportStatus;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.enums.ContributionType;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitReportRepository;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.repository.UserContributionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitReportAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BenefitReportRepository benefitReportRepository;
    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;
    private final BenefitRepository benefitRepository;
    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;
    private final UserContributionRepository userContributionRepository;

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
            report.setCinema(cinemaRepository.getReferenceById(request.cinemaId()));
        }

        if (request.movieId() != null) {
            if (!movieRepository.existsById(request.movieId())) {
                throw ExceptionUtils.badRequest("找不到電影ID：" + request.movieId());
            }
            report.setMovie(movieRepository.getReferenceById(request.movieId()));
        }
        
        // 勾選的影城先記在matchedCinemaIds（逗號分隔，格式同爬蟲），核准時才真正綁定
        if (request.cinemaIds() != null) {
            for (Integer cid : request.cinemaIds()) {
                if (!cinemaRepository.existsById(cid)) {
                    throw ExceptionUtils.badRequest("找不到影城ID：" + cid);
                }
            }
            report.setMatchedCinemaIds(request.cinemaIds().isEmpty() ? null
                    : String.join(",", request.cinemaIds().stream().map(String::valueOf).toList()));
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
        
        // 駁回理由：有填就使用管理員填寫的內容，沒填則使用預設文字
        if (request != null
                && request.reviewNote() != null
                && !request.reviewNote().isBlank()) {
            report.setReviewNote(request.reviewNote().trim());
        } else {
            report.setReviewNote("審核未通過");
        }

        return BenefitReportAdminResponse.from(benefitReportRepository.save(report));
    }

    private BenefitReportBean getOrThrow(Integer id) {
        return benefitReportRepository.findById(id)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到特典回報：" + id));
    }

    /** 取得待審核的回報；已處理過的回傳409。approve也可共用 */
    private BenefitReportBean getPendingOrThrow(Integer id) {
        BenefitReportBean report = getOrThrow(id);
        if (!BenefitReportStatus.PENDING.name().equals(report.getStatus())) {
            throw ExceptionUtils.conflict("該回報已處理，無法再修改、駁回或核准");
        }
        return report;
    }
    
    /** 核准特典回報：建立（或沿用）特典、綁定影城、記貢獻，全部成功才存 */
    @Transactional
    public BenefitReportAdminResponse approve(Integer id, BenefitReportApproveRequest request) {
        BenefitReportBean report = getPendingOrThrow(id);

        // 1. 電影：管理員指定 > 回報單原本的 > 用自訂名稱新建
        MovieBean movie = report.getMovie();
        if (request != null && request.movieId() != null) {
            movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() -> ExceptionUtils.badRequest("找不到電影ID：" + request.movieId()));
        }
        if (movie == null && report.getCustomMovieName() != null
                && !report.getCustomMovieName().isBlank()) {
            if (request == null || request.releaseDate() == null) {
                throw ExceptionUtils.badRequest("資料庫沒有這部電影，請填上映日期(releaseDate)以建立新電影");
            }
            MovieBean newMovie = new MovieBean();
            newMovie.setMovieName(report.getCustomMovieName());
            newMovie.setReleaseDate(request.releaseDate());
            movie = movieRepository.save(newMovie);
        }
        if (movie == null) {
            throw ExceptionUtils.badRequest("缺少電影，請填 movieId");
        }

        // 2. 影城：管理員選的，沒選就用回報單上的那一間
        Set<CinemaBean> cinemas = new LinkedHashSet<>();
        if (request != null && request.cinemaIds() != null) {
            for (Integer cid : request.cinemaIds()) {
                cinemas.add(cinemaRepository.findById(cid)
                        .orElseThrow(() -> ExceptionUtils.badRequest("找不到影城ID：" + cid)));
            }
        }
        if (cinemas.isEmpty() && report.getCinema() != null) {
            cinemas.add(report.getCinema());
        }
        if (cinemas.isEmpty()) {
            throw ExceptionUtils.badRequest("缺少影城，請填cinemaIds");
        }

        // 3. 特典本身的必填資料
        if (report.getBenefitName() == null || report.getBenefitName().isBlank()) {
            throw ExceptionUtils.badRequest("缺少特典名稱，請先用PATCH補上");
        }
        if (report.getStartDate() == null) {
            throw ExceptionUtils.badRequest("缺少開始日期，請先用PATCH補上");
        }
        if (report.getBenefitType() == null) {
            throw ExceptionUtils.badRequest("缺少特典類型，請先用PATCH補上");
        }

        // 4. 沿用已有的同名特典，沒有才新建
        BenefitBean benefit = benefitRepository
                .findApprovedByMovieAndName(movie.getMovieId(), report.getBenefitName())
                .stream()
                .findFirst()
                .orElseGet(() -> createBenefit(report));

        // 5. 每間影城建一筆中介表（已存在就跳過）
        for (CinemaBean cinema : cinemas) {
            if (cinemaMovieBenefitRepository
                    .existsByCinema_CinemaIdAndMovie_MovieIdAndBenefit_BenefitId(
                            cinema.getCinemaId(), movie.getMovieId(), benefit.getBenefitId())) {
                continue;
            }
            CinemaMovieBenefitBean link = new CinemaMovieBenefitBean();
            link.setCinema(cinema);
            link.setMovie(movie);
            link.setBenefit(benefit);
            cinemaMovieBenefitRepository.save(link);
        }

        // 6. 只有「使用者回報」才加貢獻（爬蟲建的回報 user 是空的）
        if (report.getUser() != null) {
            UserContributionBean contribution = new UserContributionBean();
            contribution.setUser(report.getUser());
            contribution.setContributionType(ContributionType.BENEFIT_REPORT.name());
            contribution.setReferenceId(report.getBenefitReportId());
            userContributionRepository.save(contribution);
        }

        // 7. 更新回報單
        report.setStatus(BenefitReportStatus.APPROVED.name());
        report.setMovie(movie);
        report.setBenefit(benefit);
        if (cinemas.size() == 1) {
            report.setCinema(cinemas.iterator().next());
        }
        return BenefitReportAdminResponse.from(benefitReportRepository.save(report));
    }

    /** 依回報單內容建立正式特典 */
    private BenefitBean createBenefit(BenefitReportBean report) {
        BenefitBean benefit = new BenefitBean();
        benefit.setBenefitName(report.getBenefitName());
        benefit.setIsMemberOnly(report.getIsMemberOnly());
        benefit.setBenefitType(report.getBenefitType());
        benefit.setStartDate(report.getStartDate());
        benefit.setEndDate(report.getEndDate());
        benefit.setOfficialImageUrl(report.getOfficialImageUrl());
        benefit.setPublicId(report.getPublicId());
        benefit.setSourceUrl(report.getSourceUrl());

        // 結束日已經過了就直接設為已下架
        boolean ended = report.getEndDate() != null
                && report.getEndDate().isBefore(LocalDateTime.now());
        benefit.setStatus((ended ? BenefitStatus.EXPIRED : BenefitStatus.APPROVED).name());

        return benefitRepository.save(benefit);
    }
}
