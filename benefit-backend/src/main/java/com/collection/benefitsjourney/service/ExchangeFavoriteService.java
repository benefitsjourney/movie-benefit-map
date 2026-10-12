package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.ExchangeFavoriteBean;
import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.ExchangeFavoriteRepository;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeFavoriteService {

	private final ExchangeFavoriteRepository exchangeFavoriteRepository;
	private final ExchangeListingRepository exchangeListingRepository;
	private final UsersRepository usersRepository;

	// 新增收藏
	@Transactional
	public ExchangeFavoriteBean addFavorite(
			Integer usersId,
			Integer exchangeListingId) {

		// 1. 找到會員
		UsersBean usersBean = usersRepository.findById(usersId)
				.orElseThrow(() -> new IllegalArgumentException(
						"找不到會員"));

		// 2. 找到交換刊登
		ExchangeListingBean listing = exchangeListingRepository.findById(
				exchangeListingId)
				.orElseThrow(() -> new IllegalArgumentException(
						"找不到交換刊登"));

		// 3. 只能收藏 ACTIVE 刊登
		if (!"ACTIVE".equals(listing.getStatus())) {
			throw new IllegalArgumentException(
					"目前無法收藏此交換刊登");
		}

		// 4. 防止重複收藏，檢查是否重複收藏
		boolean alreadyFavorite = exchangeFavoriteRepository
				.existsByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(
						usersId, exchangeListingId);

		if (alreadyFavorite) {
			throw new IllegalArgumentException(
					"已經收藏過此交換刊登");
		}

		// 5. 建立收藏
		ExchangeFavoriteBean favorite = new ExchangeFavoriteBean();

		favorite.setUsersBean(usersBean);
		favorite.setExchangeListingBean(listing);

		// 6. 儲存
		return exchangeFavoriteRepository.save(favorite);
	}

	// 取消收藏
	@Transactional
	public void removeFavorite(
			Integer usersId,
			Integer exchangeListingId) {

		// 1. 依會員 ID 及刊登 ID 找到收藏
		ExchangeFavoriteBean favorite = exchangeFavoriteRepository
				.findByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(
						usersId, exchangeListingId)
				.orElseThrow(() -> new IllegalArgumentException("找不到收藏紀錄"));

		// 2. 只能刪除符合目前會員 ID 的收藏
		exchangeFavoriteRepository.delete(favorite);
	}

	// 查詢自己的收藏
	public List<ExchangeFavoriteBean> findMyFavorites(Integer usersId) {
		return exchangeFavoriteRepository.findByUsersBean_UsersId(usersId);
	}
}
