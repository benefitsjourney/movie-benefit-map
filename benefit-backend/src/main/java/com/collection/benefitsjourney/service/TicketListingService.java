package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.TicketListingBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.repository.TicketListingRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketListingService {

	private final TicketListingRepository ticketListingRepository;
	private final UsersRepository usersRepository;
	private final CinemaRepository cinemaRepository;
	private final MovieRepository movieRepository;

	// 新增票券刊登
	@Transactional
	public TicketListingBean createListing(
			Integer usersId,
			String title,
			String description,
			Integer cinemaId,
			Integer movieId,
			LocalDateTime ticketShowtime) {

		// 1. 找到會員
		UsersBean usersBean = usersRepository.findById(usersId)
				.orElseThrow(() -> new IllegalArgumentException("找不到會員"));

		// 2. 找到影城
		CinemaBean cinemaBean = cinemaRepository.findById(cinemaId)
				.orElseThrow(() -> new IllegalArgumentException("找不到影城"));

		// 3. 找到電影
		MovieBean movieBean = movieRepository.findById(movieId)
				.orElseThrow(() -> new IllegalArgumentException("找不到電影"));

		// 4. 建立票券刊登
		TicketListingBean listing = new TicketListingBean();

		listing.setTitle(title);
		listing.setDescription(description);
		listing.setUsersBean(usersBean);
		listing.setCinemaBean(cinemaBean);
		listing.setMovieBean(movieBean);
		listing.setTicketShowtime(ticketShowtime);
		// 新增時預設 ACTIVE
		listing.setStatus("ACTIVE");

		// 5. 儲存
		return ticketListingRepository.save(listing);
	}

	// 查詢公開票券刊登，只顯示 ACTIVE
	public List<TicketListingBean> findActiveListings() {
		return ticketListingRepository.findByStatus("ACTIVE");
	}

	// 查詢單筆票券刊登，內部查詢：依 ID 查詢，不限制狀態
	public TicketListingBean findById(Integer ticketListingId) {
		return ticketListingRepository.findById(ticketListingId)
				.orElseThrow(() -> new IllegalArgumentException("找不到票券刊登"));
	}

	// 公開查詢單筆票券刊登，只允許 ACTIVE
	public TicketListingBean findActiveById(Integer ticketListingId) {
		return ticketListingRepository
				.findByTicketListingIdAndStatus(ticketListingId, "ACTIVE")
				.orElseThrow(() -> new IllegalArgumentException("找不到可公開查看的票券刊登"));
	}

	// 查詢自己的所有票券刊登
	public List<TicketListingBean> findMyListings(Integer usersId) {
		return ticketListingRepository.findByUsersBean_UsersId(usersId);
	}

	// 修改自己的票券刊登
	@Transactional
	public TicketListingBean updateListing(
			Integer usersId,
			Integer ticketListingId,
			String title,
			String description,
			Integer cinemaId,
			Integer movieId,
			LocalDateTime ticketShowtime) {

		// 1. 找到刊登
		TicketListingBean listing = findById(ticketListingId);

		// 2. 確認是不是自己的
		checkOwner(usersId, listing);

		// 3. DELETED 的刊登不能修改
		if ("DELETED".equals(listing.getStatus())) {
			throw new IllegalArgumentException("已刪除的刊登不能修改");
		}

		// 4. 找到新的影城
		CinemaBean cinemaBean = cinemaRepository.findById(cinemaId)
				.orElseThrow(() -> new IllegalArgumentException("找不到影城"));

		// 5. 找新的電影
		MovieBean movieBean = movieRepository.findById(movieId)
				.orElseThrow(() -> new IllegalArgumentException("找不到電影"));

		// 6. 修改資料
		listing.setTitle(title);
		listing.setDescription(description);
		listing.setCinemaBean(cinemaBean);
		listing.setMovieBean(movieBean);
		listing.setTicketShowtime(ticketShowtime);

		// 7. 儲存
		return ticketListingRepository.save(listing);
	}

	// 修改自己的票券刊登狀態
	@Transactional
	public TicketListingBean updateStatus(
			Integer usersId,
			Integer ticketListingId,
			String status) {

		// 1. 找到刊登
		TicketListingBean listing = findById(ticketListingId);

		// 2. 確認是不是自己的
		checkOwner(usersId, listing);

		// 3. 檢查狀態是否合法
		if (!"ACTIVE".equals(status)
				&& !"HIDDEN".equals(status)
				&& !"CLOSED".equals(status)
				&& !"DELETED".equals(status)) {

			throw new IllegalArgumentException(
					"無效的票券刊登狀態");
		}

		// 4. DELETED 後不能重新開啟
		if ("DELETED".equals(listing.getStatus())
				&& !"DELETED".equals(status)) {

			throw new IllegalArgumentException(
					"已刪除的刊登不能重新開啟");
		}

		// 5. 修改狀態，更新狀態
		listing.setStatus(status);

		// 6. 儲存
		return ticketListingRepository.save(listing);
	}

	// 確認是不是自己的票券刊登
	private void checkOwner(
			Integer usersId,
			TicketListingBean listing) {

		if (listing.getUsersBean() == null
				|| !usersId.equals(
						listing.getUsersBean().getUsersId())) {

			throw new IllegalArgumentException(
					"無權操作其他會員的票券刊登");
		}
	}

}
