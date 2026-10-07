package com.collection.benefitsjourney.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UseBenefitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExchangeListingService {

	private final ExchangeListingRepository exchangeListingRepository;
	
	private final UseBenefitRepository useBenefitRepository;

	// 新增刊登申請資料輸入
	public ExchangeListingBean createListing(String title , String description , Integer useBenefitId) {
		
		//找 UseBenefit
		Optional <UseBenefitBean> optionalUseBenefit = useBenefitRepository.findById(useBenefitId);
		
		// 找不到就不建立
		if(optionalUseBenefit.isEmpty()) {
			return null;
		}
		
		// 取得 UseBenefit
		UseBenefitBean useBenefitBean = optionalUseBenefit.get();
		
		// 確認這筆 UseBenefit 是否已經有交換刊登
		Optional <ExchangeListingBean> optionalExchangeListing = exchangeListingRepository.findByUseBenefitBean(useBenefitBean);
		
		// 已經有刊登，就不再建立
		if(optionalExchangeListing.isPresent()) {
			return null;
		}
		
		// 建立 ExchangeListing
		ExchangeListingBean exchangeListing = new ExchangeListingBean();
		exchangeListing.setTitle(title);
		exchangeListing.setDescription(description);
		
		//成功建立後的系統預設值，為 ACTIVE(開啟狀態)
		exchangeListing.setStatus("ACTIVE"); 
		exchangeListing.setUseBenefitBean(useBenefitBean);
		
		return exchangeListingRepository.save(exchangeListing);
	}
	

	// 刪除 刊登資料
	public void exchangeListingDelete(Integer exchangeListingId) {
		exchangeListingRepository.deleteById(exchangeListingId);
	}
}
