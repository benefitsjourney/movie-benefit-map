package com.collection.benefitsjourney.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.dto.ExchangeListingRequest;
import com.collection.benefitsjourney.service.ExchangeListingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/exchange-listings")
@RequiredArgsConstructor
public class ExchangeListingController {

	private final ExchangeListingService exchangeListingService;
	
//	// 新增交換刊登
//	@PostMapping("/create")
//	public ExchangeListingBean createExchangeListing(
//			@RequestParam String title,
//			@RequestParam String description,
//			@RequestParam Integer useBenefitId) {
//		
//		return exchangeListingService.createListing(title, description, useBenefitId);
//	}
	
	// 新增交換刊登
		@PostMapping("/create")
		public ExchangeListingBean createExchangeListing(
				@RequestBody ExchangeListingRequest request) {
			
			return exchangeListingService.createListing(
	                request.getTitle(),
	                request.getDescription(),
	                request.getUseBenefitId());
		}
		
	// 刪除交換刊登
    @DeleteMapping("/delete/{exchangeListingId}")
    public void exchangeListingDelete(@PathVariable Integer exchangeListingId) {

        exchangeListingService.exchangeListingDelete(exchangeListingId);
    }

}
