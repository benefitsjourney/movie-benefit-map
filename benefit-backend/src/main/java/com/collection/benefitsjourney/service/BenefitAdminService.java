package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.dto.BenefitResponse;
import com.collection.benefitsjourney.dto.BenefitUpdateRequest;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;

import lombok.RequiredArgsConstructor;

/** 管理員端：修改正式特典、上下架、刪除 */
@Service
@RequiredArgsConstructor
public class BenefitAdminService {

    private final BenefitRepository benefitRepository;

    private BenefitBean getOrThrow(Integer benefitId) {
        return benefitRepository.findById(benefitId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到特典：" + benefitId));
    }

    /** 修改正式特典，只更新有帶的欄位；不處理status（上下架另有API） */
    @Transactional
    public BenefitResponse update(Integer benefitId, BenefitUpdateRequest request) {
        if (request == null) {
            throw ExceptionUtils.badRequest("請求內容不可為空");
        }
        
        if ((request.officialImageUrl() != null) != (request.publicId() != null)) {
            throw ExceptionUtils.badRequest("修改圖片時，官方圖片網址與圖片publicId必須一起提供");
        }
        
        if (request.benefitName() != null && request.benefitName().isBlank()) {
            throw ExceptionUtils.badRequest("特典名稱不可為空");
        }

        BenefitBean benefit = getOrThrow(benefitId);

        if (request.benefitName() != null) {
            benefit.setBenefitName(request.benefitName().trim());
        }
        if (request.isMemberOnly() != null) {
            benefit.setIsMemberOnly(request.isMemberOnly());
        }
        if (request.benefitType() != null) {
            benefit.setBenefitType(request.benefitType().name());
        }
        if (request.startDate() != null) {
            benefit.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            benefit.setEndDate(request.endDate());
        }
        if (request.officialImageUrl() != null) {
            benefit.setOfficialImageUrl(request.officialImageUrl());
        }
        if (request.publicId() != null) {
            benefit.setPublicId(request.publicId());
        }
        if (request.sourceUrl() != null) {
            benefit.setSourceUrl(request.sourceUrl());
        }

        // 合併後再檢查，避免只改其中一個日期造成結束日早於開始日
        if (benefit.getStartDate() != null
                && benefit.getEndDate() != null
                && benefit.getEndDate().isBefore(benefit.getStartDate())) {
            throw ExceptionUtils.badRequest("下架時間不可早於開放領取時間");
        }

        return BenefitResponse.from(benefitRepository.save(benefit));
    }

    /** 上下架特典：APPROVED（已上架）／EXPIRED（已下架） */
    @Transactional
    public BenefitResponse updateStatus(Integer benefitId, BenefitStatus status) {
    	
    	if (status != BenefitStatus.APPROVED && status != BenefitStatus.EXPIRED) {
    	    throw ExceptionUtils.badRequest("特典狀態只能設定為APPROVED或EXPIRED");
    	}
    	
        BenefitBean benefit = getOrThrow(benefitId);

        // 狀態沒變就直接回傳，不重複寫入
        if (status.name().equals(benefit.getStatus())) {
            return BenefitResponse.from(benefit);
        }

        // 結束日已過的特典不能直接重新上架，避免被排程立刻又下架
        if (status == BenefitStatus.APPROVED
                && benefit.getEndDate() != null
                && benefit.getEndDate().isBefore(LocalDateTime.now())) {
            throw ExceptionUtils.badRequest("結束日期已過，請先修改結束日期再重新上架");
        }

        benefit.setStatus(status.name());
        return BenefitResponse.from(benefitRepository.save(benefit));
    }

    /**
     * 刪除特典（僅用於誤植或重複資料，一般請用下架）
     * 有會員持有紀錄用到就不能刪（409）
     * 其他關聯資料（庫存回報、貢獻、中介表）會一起清掉，全部在同一個交易，失敗就整體Rollback
     */
    @Transactional
    public void delete(Integer benefitId) {
        getOrThrow(benefitId);

        if (benefitRepository.countUseBenefits(benefitId) > 0) {
            throw ExceptionUtils.conflict("已有會員持有此特典，無法刪除，請改用下架");
        }

        benefitRepository.deleteInventoryContributionsByBenefit(benefitId);
        benefitRepository.deleteInventoryReportsByBenefit(benefitId);
        benefitRepository.deleteLinksByBenefit(benefitId);
        benefitRepository.detachReportsFromBenefit(benefitId);
        benefitRepository.deleteById(benefitId);
    }
}
