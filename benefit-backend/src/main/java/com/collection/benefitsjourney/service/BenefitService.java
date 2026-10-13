package com.collection.benefitsjourney.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.dto.BenefitResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final MovieRepository movieRepository;
    private static final int MAX_PAGE_SIZE = 100;

    /** 查詢已上架特典清單（只查APPROVED，開放日新到舊），頁碼從1開始 */
    public PageResponse<BenefitResponse> getApprovedBenefits(int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "startDate")
                .and(Sort.by(Sort.Direction.DESC, "benefitId"));

        Page<BenefitResponse> result = benefitRepository
                .findByStatus(
                        BenefitStatus.APPROVED.name(),
                        toPageable(page, size, sort)
                )
                .map(BenefitResponse::from);

        return PageResponse.from(result);
    }

    /** 查詢單一特典詳情 */
    public BenefitResponse getBenefit(Integer benefitId) {
        BenefitBean bean = benefitRepository.findById(benefitId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到特典：" + benefitId));
        return BenefitResponse.from(bean);
    }

    /** 查某部電影有哪些特典 */
    public PageResponse<BenefitResponse> getBenefitsByMovie(
            Integer movieId, BenefitStatus status, int page, int size) {

        if (!movieRepository.existsById(movieId)) {
            throw ExceptionUtils.notFound("找不到電影 ID：" + movieId);
        }

        Page<BenefitResponse> result = benefitRepository
                .findByMovieId(movieId, status.name(),
                        toPageable(page, size, Sort.unsorted()))
                .map(BenefitResponse::from);

        return PageResponse.from(result);
    }
    

    /** 頁碼最小 1、每頁最多 100 筆 */
    private Pageable toPageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage - 1, safeSize, sort);
    }
}