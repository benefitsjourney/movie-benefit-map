package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.dto.UseBenefitCreateRequest;
import com.collection.benefitsjourney.dto.UseBenefitResponse;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UseBenefitRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/** 持有紀錄（收藏庫）：新增、查自己的清單、刪除 */
@Service
@RequiredArgsConstructor
public class UseBenefitService {

    private final UseBenefitRepository useBenefitRepository;
    private final BenefitRepository benefitRepository;
    private final UsersRepository usersRepository;
    private final ExchangeListingRepository exchangeListingRepository;

    /**
     * 新增持有紀錄。
     * 已上架、已下架的特典都可以新增。
     * 配合交換設計，同一個特典只能有一筆。
     */
    @Transactional
    public UseBenefitResponse create(Integer usersId, UseBenefitCreateRequest request) {
        BenefitBean benefit = benefitRepository.findById(request.benefitId())
                .orElseThrow(() -> ExceptionUtils.notFound("找不到特典：" + request.benefitId()));

        if (useBenefitRepository.existsByUser_UsersIdAndBenefit_BenefitId(usersId, request.benefitId())) {
            throw ExceptionUtils.conflict("已經收藏過這個特典");
        }

        UseBenefitBean useBenefit = new UseBenefitBean();
        // 只需要會員ID，不用另外查詢會員的完整資料
        useBenefit.setUser(usersRepository.getReferenceById(usersId));
        useBenefit.setBenefit(benefit);

        return UseBenefitResponse.from(useBenefitRepository.save(useBenefit));
    }

    /** 查登入者自己的持有清單（新到舊，不分特典上下架） */
    @Transactional(readOnly = true)
    public List<UseBenefitResponse> findMine(Integer usersId) {
        return useBenefitRepository.findMyUseBenefits(usersId)
                .stream()
                .map(UseBenefitResponse::from)
                .toList();
    }

    /**
     * 刪除持有紀錄。
     * 只能刪自己的，已經拿去建立交換刊登的不能刪（交換刊登綁著這筆持有紀錄）。
     * 照片資料列會跟著一起刪（UseBenefitBean的photos有設orphanRemoval）。
     * TODO 照片API：刪除前要先把Cloudinary上的照片檔也刪掉
     */
    @Transactional
    public void delete(Integer usersId, Integer useBenefitId) {
        UseBenefitBean useBenefit = getMineOrThrow(usersId, useBenefitId);

        if (exchangeListingRepository.findByUseBenefitBean(useBenefit).isPresent()) {
            throw ExceptionUtils.conflict("這筆持有紀錄已建立交換刊登，無法刪除");
        }

        useBenefitRepository.delete(useBenefit);
    }

    /** 找持有紀錄，並確認是登入者自己的 */
    UseBenefitBean getMineOrThrow(Integer usersId, Integer useBenefitId) {
        UseBenefitBean useBenefit = useBenefitRepository.findById(useBenefitId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到持有紀錄：" + useBenefitId));

        if (!useBenefit.getUser().getUsersId().equals(usersId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能操作自己的持有紀錄");
        }
        return useBenefit;
    }
}