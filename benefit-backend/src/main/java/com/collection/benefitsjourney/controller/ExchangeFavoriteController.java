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

import com.collection.benefitsjourney.domain.ExchangeFavoriteBean;
import com.collection.benefitsjourney.dto.ExchangeFavoriteResponse;
import com.collection.benefitsjourney.service.ExchangeFavoriteService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/exchange-favorites")
@RequiredArgsConstructor
public class ExchangeFavoriteController {

	private final ExchangeFavoriteService exchangeFavoriteService;

	// 收藏一筆交換刊登
	@PostMapping("/{exchangeListingId}")
	public ResponseEntity<ExchangeFavoriteResponse> addFavorite(
			@PathVariable Integer exchangeListingId,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		ExchangeFavoriteBean favorite = exchangeFavoriteService.addFavorite(
				usersId,
				exchangeListingId);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ExchangeFavoriteResponse.from(favorite));
	}

	// 取消自己對指定交換刊登的收藏
	@DeleteMapping("/{exchangeListingId}")
	public ResponseEntity<Void> removeFavorite(
			@PathVariable Integer exchangeListingId,
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		exchangeFavoriteService.removeFavorite(
				usersId,
				exchangeListingId);

		return ResponseEntity.noContent().build();
	}

	// 查詢目前登入會員自己的交換收藏
	@GetMapping("/my")
	public ResponseEntity<List<ExchangeFavoriteResponse>> findMyFavorites(
			HttpServletRequest request) {

		Integer usersId = getUsersId(request);

		List<ExchangeFavoriteResponse> responses = exchangeFavoriteService.findMyFavorites(usersId)
				.stream()
				.map(ExchangeFavoriteResponse::from)
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