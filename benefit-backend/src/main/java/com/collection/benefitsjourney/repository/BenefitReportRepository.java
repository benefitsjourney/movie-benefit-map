package com.collection.benefitsjourney.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.BenefitReportBean;

public interface BenefitReportRepository extends JpaRepository<BenefitReportBean, Integer> {

    /** GET/benefit-reports?status=PENDING，查詢所有待審核的特典（升序，先送先審）*/
	Page<BenefitReportBean> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

	/** GET/benefit-reports/me，使用者查看自己回報缺漏的特典狀態（按建立時間降序，同時間則按回報ID降序）*/
	Page<BenefitReportBean> findByUser_UsersIdOrderByCreatedAtDescBenefitReportIdDesc(Integer userId, Pageable pageable);

    /** 爬蟲去重（同貼文+同特典名稱），避免同一則貼文被重複送審 */
    boolean existsBySourceUrlAndBenefitName(String sourceUrl, String benefitName);
    
    /** INCOMPLETE貼文沒有特典名，要用貼文網址去重 */
    boolean existsBySourceUrl(String sourceUrl);

    /** 重複匯入同一則貼文時，替還沒有圖片的回報單補圖 */
    List<BenefitReportBean> findBySourceUrl(String sourceUrl);
    
    List<BenefitReportBean> findBySourceUrlAndBenefitName(String sourceUrl, String benefitName);
    

    /** 爬蟲去重（同電影＋同特典名稱＋同開始日）：不同貼文重複宣傳同一個特典時，避免重複送審 */
    boolean existsByMovie_MovieIdAndBenefitNameAndStartDate(Integer movieId, String benefitName, LocalDateTime startDate);

    /** 重新比對電影：找出爬蟲建立（沒有使用者）、還在待審核的回報單 */
    List<BenefitReportBean> findByStatusAndUserIsNull(String status);
}