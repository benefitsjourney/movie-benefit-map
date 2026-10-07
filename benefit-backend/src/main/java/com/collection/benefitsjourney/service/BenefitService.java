package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.dto.BenefitResponse;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitService {

    private final BenefitRepository benefitRepository;

    /** 查詢已上架特典清單（只查APPROVED，開放日新到舊） */
    public List<BenefitResponse> getApprovedBenefits(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        return benefitRepository
                .findByStatus(BenefitStatus.APPROVED.name(), pageable)
                .stream()
                .map(BenefitResponse::from)
                .toList();
    }

    /** 查詢單一特典詳情 */
    public BenefitResponse getBenefit(Integer benefitId) {
        BenefitBean bean = benefitRepository.findById(benefitId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到特典：" + benefitId));
        return BenefitResponse.from(bean);
    }

    /** 查某部電影有哪些特典（預設只回已上架，排序已寫在Repository的JPQL，這裡不再加Sort） */
    public List<BenefitResponse> getBenefitsByMovie(Integer movieId, String status, int page, int size) {
        // TODO: Movie 合併後，驗證 movieId 是否存在，不存在回 404
        String statusName = parseStatus(status);
        Pageable pageable = PageRequest.of(page, size);
        return benefitRepository
                .findByMovieId(movieId, statusName, pageable)
                .stream()
                .map(BenefitResponse::from)
                .toList();
    }

    /** 驗證status字串是否為合法的BenefitStatus */
    private String parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return BenefitStatus.APPROVED.name();
        }
        try {
            return BenefitStatus.valueOf(status.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw ExceptionUtils.badRequest("status只能是APPROVED或EXPIRED");
        }
    }
}