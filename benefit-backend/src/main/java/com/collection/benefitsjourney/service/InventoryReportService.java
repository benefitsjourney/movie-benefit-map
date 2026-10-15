package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.InventoryReportBean;
import com.collection.benefitsjourney.domain.UserContributionBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.InventoryReportCreateRequest;
import com.collection.benefitsjourney.dto.InventoryReportResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.enums.ContributionType;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.InventoryReportRepository;
import com.collection.benefitsjourney.repository.UserContributionRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/** 庫存回報：新增回報、查看自己的回報 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryReportService {

    /** 同一位使用者對同一個影城特典，兩次回報至少隔15分鐘的冷卻時間 */
    static final int COOLDOWN_MINUTES = 15;
    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryReportRepository inventoryReportRepository;
    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;
    private final UsersRepository usersRepository;
    private final UserContributionRepository userContributionRepository;

    /**
     * 新增庫存回報。同一個交易內完成：
     * 1. 新增InventoryReport
     * 2. 更新中介表benefit_inventory_status
     * 3. 新增UserContribution
     * 任一步失敗就整體Rollback
     */
    @Transactional
    public InventoryReportResponse create(Integer usersId, InventoryReportCreateRequest request) {
        UsersBean user = usersRepository.findById(usersId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到會員"));
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此帳號目前無法回報");
        }

        // 有庫存／即將售罄／已售罄
        if (request.status() == BenefitInventoryStatus.UNKNOWN) {
            throw ExceptionUtils.badRequest("庫存狀態不可回報為 UNKNOWN");
        }

        CinemaMovieBenefitBean link = cinemaMovieBenefitRepository
                .findById(request.cinemaMovieBenefitId())
                .orElseThrow(() -> ExceptionUtils.notFound(
                        "找不到影城特典：" + request.cinemaMovieBenefitId()));

        if (!BenefitStatus.APPROVED.name().equals(link.getBenefit().getStatus())) {
            throw ExceptionUtils.conflict("此特典已下架，無法回報庫存");
        }

        // 回報頻率限制
        LocalDateTime since = LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES);
        if (inventoryReportRepository
                .existsByUser_UsersIdAndCinemaMovieBenefit_CinemaMovieBenefitIdAndReportedAtAfter(
                        usersId, link.getCinemaMovieBenefitId(), since)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "請間隔 " + COOLDOWN_MINUTES + " 分鐘後再回報");
        }

        // 1. 歷史紀錄
        InventoryReportBean report = new InventoryReportBean();
        report.setUser(user);
        report.setCinemaMovieBenefit(link);
        report.setStatus(request.status().getCode());
        report = inventoryReportRepository.saveAndFlush(report);

        // 2. 中介表狀態=「回報時間最新」那筆的狀態
        //    不直接寫入這次的值（兩筆回報同時commit時，後commit的不一定是較晚回報的）
        InventoryReportBean latest = inventoryReportRepository
                .findFirstByCinemaMovieBenefit_CinemaMovieBenefitIdOrderByReportedAtDescReportIdDesc(
                        link.getCinemaMovieBenefitId())
                .orElse(report);
        link.setBenefitInventoryStatus(latest.getStatus());
        cinemaMovieBenefitRepository.save(link);

        // 3. 貢獻紀錄
        UserContributionBean contribution = new UserContributionBean();
        contribution.setUser(user);
        contribution.setContributionType(ContributionType.INVENTORY_REPORT.name());
        contribution.setReferenceId(report.getReportId());
        userContributionRepository.save(contribution);

        return InventoryReportResponse.from(report);
    }

    /** 查看自己的庫存回報（新到舊），頁碼從1開始 */
    public PageResponse<InventoryReportResponse> findMyReports(Integer usersId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<InventoryReportResponse> result = inventoryReportRepository
                .findMyReports(usersId, PageRequest.of(safePage - 1, safeSize))
                .map(InventoryReportResponse::from);

        return PageResponse.from(result);
    }
}
