package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.TicketFavoriteBean;
import com.collection.benefitsjourney.dto.TicketFavoriteResponse;
import com.collection.benefitsjourney.service.TicketFavoriteService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ticket-favorites")
@RequiredArgsConstructor
public class TicketFavoriteController {

	private final TicketFavoriteService ticketFavoriteService;

	// 收藏一筆票券刊登
	@PostMapping("/{ticketListingId}")
	public ResponseEntity<TicketFavoriteResponse> addFavorite(
			@PathVariable Integer ticketListingId,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		TicketFavoriteBean favorite = ticketFavoriteService.addFavorite(
				usersId,
				ticketListingId);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(TicketFavoriteResponse.from(favorite));
	}

	// 取消自己對指定票券刊登的收藏
	@DeleteMapping("/{ticketListingId}")
	public ResponseEntity<Void> removeFavorite(
			@PathVariable Integer ticketListingId,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		ticketFavoriteService.removeFavorite(
				usersId,
				ticketListingId);

		return ResponseEntity.noContent().build();
	}

	// 查詢目前登入會員自己的票券收藏
	@GetMapping("/my")
	public ResponseEntity<List<TicketFavoriteResponse>> findMyFavorites(
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		List<TicketFavoriteResponse> responses = ticketFavoriteService.findMyFavorites(usersId)
				.stream()
				.map(TicketFavoriteResponse::from)
				.toList();

		return ResponseEntity.ok(responses);
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