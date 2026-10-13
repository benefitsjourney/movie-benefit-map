package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.TicketFavoriteBean;
import com.collection.benefitsjourney.domain.TicketListingBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.TicketFavoriteRepository;
import com.collection.benefitsjourney.repository.TicketListingRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketFavoriteService {
	private final TicketFavoriteRepository ticketFavoriteRepository;
	private final TicketListingRepository ticketListingRepository;
	private final UsersRepository usersRepository;

	// 新增收藏
	@Transactional
	public TicketFavoriteBean addFavorite(
			Integer usersId,
			Integer ticketListingId) {

		// 1. 找到會員
		UsersBean usersBean = usersRepository.findById(usersId)
				.orElseThrow(() -> new IllegalArgumentException("找不到會員"));

		// 2. 找到票券刊登
		TicketListingBean listing = ticketListingRepository.findById(ticketListingId)
				.orElseThrow(() -> new IllegalArgumentException("找不到票券刊登"));

		// 3. 只能收藏 ACTIVE
		if (!"ACTIVE".equals(listing.getStatus())) {
			throw new IllegalArgumentException("目前無法收藏此票券刊登");
		}

		// 4. 防止重複收藏，檢查是否重複收藏
		boolean alreadyFavorite = ticketFavoriteRepository
				.existsByUsersBean_UsersIdAndTicketListingBean_TicketListingId(
						usersId, ticketListingId);

		if (alreadyFavorite) {
			throw new IllegalArgumentException("已經收藏過此票券刊登");
		}

		// 5. 建立收藏
		TicketFavoriteBean favorite = new TicketFavoriteBean();
		favorite.setUsersBean(usersBean);
		favorite.setTicketListingBean(listing);

		// 6. 儲存
		return ticketFavoriteRepository.save(favorite);
	}

	// 取消收藏，取消自己的票券收藏
	@Transactional
	public void removeFavorite(
			Integer usersId,
			Integer ticketListingId) {

		TicketFavoriteBean favorite = ticketFavoriteRepository
				.findByUsersBean_UsersIdAndTicketListingBean_TicketListingId(
						usersId, ticketListingId)
				.orElseThrow(() -> new IllegalArgumentException("找不到收藏紀錄"));

		ticketFavoriteRepository.delete(favorite);
	}

	// 查詢自己的收藏
	public List<TicketFavoriteBean> findMyFavorites(Integer usersId) {
		return ticketFavoriteRepository.findByUsersBean_UsersId(usersId);
	}
}
