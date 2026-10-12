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

import com.collection.benefitsjourney.domain.TicketListingBean;
import com.collection.benefitsjourney.dto.TicketListingRequest;
import com.collection.benefitsjourney.dto.TicketListingResponse;
import com.collection.benefitsjourney.dto.TicketListingStatusRequest;
import com.collection.benefitsjourney.dto.TicketListingUpdateRequest;
import com.collection.benefitsjourney.service.TicketListingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ticket-listings")
@RequiredArgsConstructor
public class TicketListingController {

	private final TicketListingService ticketListingService;

	// 新增票券刊登
	@PostMapping
	public ResponseEntity<TicketListingResponse> createListing(
			@Valid @RequestBody TicketListingRequest requestBody,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		TicketListingBean listing = ticketListingService.createListing(
				usersId,
				requestBody.getTitle(),
				requestBody.getDescription(),
				requestBody.getCinemaId(),
				requestBody.getMovieId(),
				requestBody.getTicketShowtime());

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(TicketListingResponse.from(listing));
	}

	// 查詢所有公開的票券刊登
	@GetMapping
	public ResponseEntity<List<TicketListingResponse>> findActiveListings() {

		List<TicketListingResponse> responses = ticketListingService.findActiveListings()
				.stream()
				.map(TicketListingResponse::from)
				.toList();

		return ResponseEntity.ok(responses);
	}

	// 查詢單筆公開的票券刊登
	@GetMapping("/{ticketListingId}")
	public ResponseEntity<TicketListingResponse> findActiveById(
			@PathVariable Integer ticketListingId) {

		TicketListingBean listing = ticketListingService.findActiveById(ticketListingId);

		return ResponseEntity.ok(
				TicketListingResponse.from(listing));
	}

	// 查詢目前登入會員自己的票券刊登
	@GetMapping("/my")
	public ResponseEntity<List<TicketListingResponse>> findMyListings(
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		List<TicketListingResponse> responses = ticketListingService.findMyListings(usersId)
				.stream()
				.map(TicketListingResponse::from)
				.toList();

		return ResponseEntity.ok(responses);
	}

	// 修改自己的票券刊登內容
	@PutMapping("/{ticketListingId}")
	public ResponseEntity<TicketListingResponse> updateListing(
			@PathVariable Integer ticketListingId,
			@Valid @RequestBody TicketListingUpdateRequest requestBody,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		TicketListingBean listing = ticketListingService.updateListing(
				usersId,
				ticketListingId,
				requestBody.getTitle(),
				requestBody.getDescription(),
				requestBody.getCinemaId(),
				requestBody.getMovieId(),
				requestBody.getTicketShowtime());

		return ResponseEntity.ok(
				TicketListingResponse.from(listing));
	}

	// 修改自己的票券刊登狀態
	@PatchMapping("/{ticketListingId}/status")
	public ResponseEntity<TicketListingResponse> updateStatus(
			@PathVariable Integer ticketListingId,
			@Valid @RequestBody TicketListingStatusRequest requestBody,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		TicketListingBean listing = ticketListingService.updateStatus(
				usersId,
				ticketListingId,
				requestBody.getStatus());

		return ResponseEntity.ok(
				TicketListingResponse.from(listing));
	}

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