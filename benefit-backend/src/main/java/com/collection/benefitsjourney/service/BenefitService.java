package com.collection.benefitsjourney.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.dto.BenefitListResponse;
import com.collection.benefitsjourney.dto.BenefitResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.LikeKeywords;
import com.collection.benefitsjourney.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final MovieRepository movieRepository;
    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;
    private static final int MAX_PAGE_SIZE = 100;

    /** 前台：查詢已上架特典清單（只查APPROVED，開放日新到舊），頁碼從1開始
     *  keyword：搜特典名稱或電影名稱；benefitType：只看某種類型。兩個都可以不帶 */
    public PageResponse<BenefitListResponse> getApprovedBenefits(
    		 String keyword, BenefitType benefitType, int page, int size) {
        return search(BenefitStatus.APPROVED, keyword, benefitType, page, size);
    }

    /** 後台：查詢所有特典，status不帶＝全部（含已下架） */
    public PageResponse<BenefitListResponse> getBenefitsForAdmin(
            BenefitStatus status, String keyword, BenefitType benefitType, int page, int size) {
        return search(status, keyword, benefitType, page, size);
    }

    /** 前後台共用：依條件查特典，並帶出電影名稱 */
    private PageResponse<BenefitListResponse> search(
    		 BenefitStatus status, String keyword, BenefitType benefitType, int page, int size) {
    	
        Sort sort = Sort.by(Sort.Direction.DESC, "startDate")
                .and(Sort.by(Sort.Direction.DESC, "benefitId"));

        Page<BenefitBean> benefits = benefitRepository.searchBenefits(
                status == null ? null : status.name(),
                LikeKeywords.toLikeKeyword(keyword),
                benefitType == null ? null : benefitType.name(),
                toPageable(page, size, sort));

        // 這一頁的特典ID→電影（一個特典通常只對應一部電影，取第一筆）
        List<Integer> ids = benefits.getContent().stream().map(BenefitBean::getBenefitId).toList();
        Map<Integer, Object[]> movies = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : cinemaMovieBenefitRepository.findMoviesByBenefitIds(ids)) {
                movies.putIfAbsent((Integer) row[0], row);
            }
        }

        Page<BenefitListResponse> result = benefits.map(b -> {
            Object[] movie = movies.get(b.getBenefitId());
            return BenefitListResponse.from(b,
                    movie == null ? null : (Integer) movie[1],
                    movie == null ? null : (String) movie[2]);
        });
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