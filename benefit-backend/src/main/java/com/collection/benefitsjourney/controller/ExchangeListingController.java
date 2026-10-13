package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.dto.ExchangeListingRequest;
import com.collection.benefitsjourney.dto.ExchangeListingResponse;
import com.collection.benefitsjourney.dto.ExchangeListingStatusRequest;
import com.collection.benefitsjourney.dto.ExchangeListingUpdateRequest;
import com.collection.benefitsjourney.service.ExchangeListingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/exchange-listings")
@RequiredArgsConstructor
public class ExchangeListingController {

	private final ExchangeListingService exchangeListingService;

	// 新增交換刊登
	@PostMapping
	public ResponseEntity<ExchangeListingResponse> createListing(
			@Valid @RequestBody ExchangeListingRequest requestBody,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		ExchangeListingBean listing = exchangeListingService.createListing(
				usersId,
				requestBody.getTitle(),
				requestBody.getDescription(),
				requestBody.getUseBenefitId());

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ExchangeListingResponse.from(listing));
	}

	// 查詢所有公開的交換刊登
	@GetMapping
	public ResponseEntity<List<ExchangeListingResponse>> findActiveListings() {

		List<ExchangeListingResponse> responses = exchangeListingService.findActiveListings()
				.stream()
				.map(ExchangeListingResponse::from)
				.toList();

		return ResponseEntity.ok(responses);
	}

	// 查詢單筆公開的交換刊登
	@GetMapping("/{exchangeListingId}")
	public ResponseEntity<ExchangeListingResponse> findActiveById(
			@PathVariable Integer exchangeListingId) {

		ExchangeListingBean listing = exchangeListingService.findActiveById(exchangeListingId);

		return ResponseEntity.ok(
				ExchangeListingResponse.from(listing));
	}

	// 查詢目前登入會員自己的交換刊登
	@GetMapping("/my")
	public ResponseEntity<List<ExchangeListingResponse>> findMyListings(
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		List<ExchangeListingResponse> responses = exchangeListingService.findMyListings(usersId)
				.stream()
				.map(ExchangeListingResponse::from)
				.toList();

		return ResponseEntity.ok(responses);
	}

	// 修改自己的交換刊登內容
	@PutMapping("/{exchangeListingId}")
	public ResponseEntity<ExchangeListingResponse> updateListing(
			@PathVariable Integer exchangeListingId,
			@Valid @RequestBody ExchangeListingUpdateRequest requestBody,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		ExchangeListingBean listing = exchangeListingService.updateListing(
				usersId,
				exchangeListingId,
				requestBody.getTitle(),
				requestBody.getDescription());

		return ResponseEntity.ok(
				ExchangeListingResponse.from(listing));
	}

	// 修改自己的交換刊登狀態
	@PatchMapping("/{exchangeListingId}/status")
	public ResponseEntity<ExchangeListingResponse> updateStatus(
			@PathVariable Integer exchangeListingId,
			@Valid @RequestBody ExchangeListingStatusRequest requestBody,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		ExchangeListingBean listing = exchangeListingService.updateStatus(
				usersId,
				exchangeListingId,
				requestBody.getStatus());

		return ResponseEntity.ok(
				ExchangeListingResponse.from(listing));
	}

	// 從 JWT Filter 放入的 request attribute 取得會員 ID
	private Integer getUsersId(HttpServletRequest request) {

		Object value = request.getAttribute("usersId");

		if (!(value instanceof Integer)) {
			throw new org.springframework.web.server.ResponseStatusException(
					HttpStatus.UNAUTHORIZED,
					"尚未取得登入會員資料");
		}

		return (Integer) value;
	}
}