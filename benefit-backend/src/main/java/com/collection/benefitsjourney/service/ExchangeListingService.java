package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;

@Service
public class ExchangeListingService {

	private ExchangeListingRepository exchangeListingRepository;

	// 找 UseBenefitBean 的資料
	public List<ExchangeListingBean> findByUseBenefitId(Integer userId) {
		return exchangeListingRepository.findByUseBenefitId(userId);
	}

	// 新增刊登資料
	public ExchangeListingBean exchangeListingCreate(String title, String Description) {
		ExchangeListingBean exchangeListing = new ExchangeListingBean();
		exchangeListing.setTitle(title);
		exchangeListing.setDescription(Description);
		// exchangeListing.getUseBenefitBean().getUserId();

		return exchangeListingRepository.save(exchangeListing);
	}

	// 刪除 刊登資料
	public void exchangeListingDelete(Integer exchangeListingId) {
		exchangeListingRepository.deleteById(exchangeListingId);
	}
}
