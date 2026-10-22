package com.collection.benefitsjourney.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.BenefitReportBean;
import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.BenefitReportCreateRequest;
import com.collection.benefitsjourney.dto.BenefitReportUserResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitReportStatus;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitReportRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/** 會員端：回報缺漏特典、查看自己的回報 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitReportService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String USER_REPORT_NOTE = "使用者回報缺漏";

    private final BenefitReportRepository benefitReportRepository;
    private final UsersRepository usersRepository;
    private final MovieRepository movieRepository;
    private final CinemaRepository cinemaRepository;

    /** 新增缺漏特典回報，狀態一律為PENDING */
    @Transactional
    public BenefitReportUserResponse create(Integer usersId, BenefitReportCreateRequest request) {
        UsersBean user = usersRepository.findById(usersId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到會員"));

        // 被停權或已停用的帳號不能回報
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此帳號目前無法回報");
        }

        BenefitReportBean report = new BenefitReportBean();
        report.setUser(user);
        report.setBenefitName(request.benefitName().trim());
        report.setOfficialImageUrl(request.officialImageUrl());
        report.setPublicId(request.publicId());
        report.setSourceUrl(request.sourceUrl());
        report.setStatus(BenefitReportStatus.PENDING.name());
        report.setReviewNote(USER_REPORT_NOTE);
        // 是否為會員限定（必填）
        report.setIsMemberOnly(request.isMemberOnly());
        if (request.benefitType() != null) {
            report.setBenefitType(request.benefitType().name());
        }
        
        // 開始日必填
        report.setStartDate(request.startDate());
        report.setEndDate(request.endDate());

        if (request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw ExceptionUtils.badRequest("結束日期不可早於開始日期");
        }

        // 電影：有id用資料庫的，沒有就一定要填自訂名稱
        if (request.movieId() != null) {
            MovieBean movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() -> ExceptionUtils.badRequest("找不到電影ID：" + request.movieId()));
            report.setMovie(movie);
        } else if (hasText(request.customMovieName())) {
            report.setCustomMovieName(request.customMovieName().trim());
        } else {
            throw ExceptionUtils.badRequest("請選擇電影，或填寫電影名稱");
        }
        

        // 影城：可勾多間（cinemaIds），也可另外填清單裡沒有的影城（customCinemaName），至少要有一個
        List<Integer> cinemaIds = new ArrayList<>();
        if (request.cinemaIds() != null) {
            request.cinemaIds().stream()
                    .filter(id -> id != null && !cinemaIds.contains(id))
                    .forEach(cinemaIds::add);
        }
        // 舊寫法只帶一個cinemaId也能用
        if (request.cinemaId() != null && !cinemaIds.contains(request.cinemaId())) {
            cinemaIds.add(0, request.cinemaId());
        }

        CinemaBean firstCinema = null;
        for (Integer cid : cinemaIds) {
            CinemaBean cinema = cinemaRepository.findById(cid)
                    .orElseThrow(() -> ExceptionUtils.badRequest("找不到影城ID：" + cid));
            if (firstCinema == null) {
                firstCinema = cinema;
            }
        }

        if (firstCinema != null) {
            // 第一間記在cinema_id（我的回報顯示用），全部記在matchedCinemaIds（格式同爬蟲，審核時預先勾選）
            report.setCinema(firstCinema);
            report.setMatchedCinemaIds(String.join(",",
                    cinemaIds.stream().map(String::valueOf).toList()));
        }
        if (hasText(request.customCinemaName())) {
            report.setCustomCinemaName(request.customCinemaName().trim());
        }
        if (firstCinema == null && !hasText(request.customCinemaName())) {
            throw ExceptionUtils.badRequest("請選擇影城，或填寫影城名稱");
        }

        return BenefitReportUserResponse.from(benefitReportRepository.save(report));
    }

    /** 查看自己回報過的特典（最新的在前面），頁碼從1開始 */
    public PageResponse<BenefitReportUserResponse> findMyReports(Integer usersId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<BenefitReportUserResponse> result = benefitReportRepository
                .findByUser_UsersIdOrderByCreatedAtDescBenefitReportIdDesc(usersId, PageRequest.of(safePage - 1, safeSize))
                .map(BenefitReportUserResponse::from);

        return PageResponse.from(result);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}