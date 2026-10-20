package com.collection.benefitsjourney.service;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.MovieBean;
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

		// 6. 儲存收藏
		favorite = ticketFavoriteRepository.save(favorite);

		// 7. 在交易結束前初始化 DTO 需要的關聯資料
		initializeListingDetails(favorite);

		return favorite;
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
		// 不依刊登狀態篩選，保留已收藏但已隱藏、結束或刪除的刊登
		List<TicketFavoriteBean> favorites = ticketFavoriteRepository.findByUsersBean_UsersId(usersId);

		// 在唯讀交易結束前，先初始化 DTO 需要的關聯資料
		favorites.forEach(this::initializeListingDetails);

		return favorites;
	}

	// 初始化票券收藏回傳資料所需的關聯
	private void initializeListingDetails(TicketFavoriteBean favorite) {

		TicketListingBean listing = favorite.getTicketListingBean();

		if (listing == null) {
			return;
		}

		// 初始化刊登
		Hibernate.initialize(listing);

		// 新增：載入票券刊登者會員資料
		UsersBean usersBean = listing.getUsersBean();

		if (usersBean != null) {
			Hibernate.initialize(usersBean);
		}

		// 初始化影城資料
		CinemaBean cinema = listing.getCinemaBean();

		if (cinema != null) {
			Hibernate.initialize(cinema);
		}

		// 初始化電影資料
		MovieBean movie = listing.getMovieBean();

		if (movie != null) {
			Hibernate.initialize(movie);
		}
	}
}
