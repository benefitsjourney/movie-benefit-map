package com.collection.benefitsjourney.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.hibernate.Hibernate;
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

	private static final String STATUS_ACTIVE = "ACTIVE";
	private static final String STATUS_HIDDEN = "HIDDEN";
	private static final String STATUS_CLOSED = "CLOSED";
	private static final String STATUS_DELETED = "DELETED";

	private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

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

		// 放映日期只能是明天或之後
		validateFutureShowtime(ticketShowtime);

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
		listing.setStatus(STATUS_ACTIVE);

		// 5. 儲存
		TicketListingBean savedListing = ticketListingRepository.save(listing);

		initializeListingUser(savedListing);

		return savedListing;
	}

	// 查詢公開票券刊登，只顯示 ACTIVE 且放映日期為明天或之後
	public List<TicketListingBean> findActiveListings() {
		LocalDateTime cutoff = getTomorrowStartOfDay();
		List<TicketListingBean> listings = ticketListingRepository.findByStatusAndTicketShowtimeGreaterThanEqual(
				STATUS_ACTIVE, cutoff);

		listings.forEach(this::initializeListingUser);

		return listings;
	}

	// 查詢單筆票券刊登，內部查詢：依 ID 查詢，不限制狀態
	public TicketListingBean findById(Integer ticketListingId) {
		TicketListingBean listing = ticketListingRepository.findById(ticketListingId)
				.orElseThrow(() -> new IllegalArgumentException("找不到票券刊登"));

		initializeListingUser(listing);

		return listing;
	}

	// 公開查詢單筆票券刊登，只允許 ACTIVE 且放映日期為明天或之後
	public TicketListingBean findActiveById(Integer ticketListingId) {
		LocalDateTime cutoff = getTomorrowStartOfDay();
		TicketListingBean listing = ticketListingRepository
				.findByTicketListingIdAndStatusAndTicketShowtimeGreaterThanEqual(
						ticketListingId, STATUS_ACTIVE, cutoff)
				.orElseThrow(() -> new IllegalArgumentException(
						"找不到可公開查看的票券刊登"));

		initializeListingUser(listing);

		return listing;
	}

	// 查詢自己的所有票券刊登，保留歷史及 DELETED 票券
	public List<TicketListingBean> findMyListings(Integer usersId) {
		List<TicketListingBean> listings = ticketListingRepository.findByUsersBean_UsersId(usersId);

		listings.forEach(this::initializeListingUser);

		return listings;
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
		if (STATUS_DELETED.equals(listing.getStatus())) {
			throw new IllegalArgumentException("已刪除的刊登不能修改");
		}

		// 4. 放映日期只能是明天或之後
		validateFutureShowtime(ticketShowtime);

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
		TicketListingBean savedListing = ticketListingRepository.save(listing);

		initializeListingUser(savedListing);

		return savedListing;
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
		if (!STATUS_ACTIVE.equals(status)
				&& !STATUS_HIDDEN.equals(status)
				&& !STATUS_CLOSED.equals(status)
				&& !STATUS_DELETED.equals(status)) {

			throw new IllegalArgumentException("無效的票券刊登狀態");
		}

		// 4. DELETED 後不能重新開啟
		if (STATUS_DELETED.equals(listing.getStatus())
				&& !STATUS_DELETED.equals(status)) {

			throw new IllegalArgumentException("已刪除的刊登不能重新開啟");
		}

		// 5. 放映日期為今天或過去時，不允許修改狀態
		if (isExpired(listing.getTicketShowtime())) {
			throw new IllegalArgumentException(
					"放映日期已到或已過，不能修改票券刊登狀態");
		}

		// 5. 修改狀態，更新狀態
		listing.setStatus(status);

		// 6. 儲存
		TicketListingBean savedListing = ticketListingRepository.save(listing);

		initializeListingUser(savedListing);

		return savedListing;
	}

	// 將放映日期為今天或過去、且尚未 DELETED 的票券改成 DELETED。
	// 由 TicketListingExpirationScheduler 呼叫。
	// @return 本次更新的票券數量
	@Transactional
	public int markExpiredListingsAsDeleted() {

		// 明天 00:00:00 作為分界
		LocalDateTime cutoff = getTomorrowStartOfDay();

		// 找出今天及過去，且尚未 DELETED 的票券
		List<TicketListingBean> expiredListings = ticketListingRepository
				.findByTicketShowtimeBeforeAndStatusNot(
						cutoff, STATUS_DELETED);

		// 沒有過期票券就不必儲存
		if (expiredListings.isEmpty()) {
			return 0;
		}

		// 將符合條件的票券改成 DELETED
		for (TicketListingBean listing : expiredListings) {
			listing.setStatus(STATUS_DELETED);
		}

		// 批次儲存
		ticketListingRepository.saveAll(expiredListings);

		return expiredListings.size();
	}

	// 驗證放映日期必須是明天或之後。
	// 只比較年月日，不比較時分秒。
	private void validateFutureShowtime(LocalDateTime ticketShowtime) {

		if (ticketShowtime == null) {
			throw new IllegalArgumentException("放映日期不可為空");
		}

		LocalDate showtimeDate = ticketShowtime.toLocalDate();
		LocalDate today = LocalDate.now(TAIPEI_ZONE);

		if (!showtimeDate.isAfter(today)) {
			throw new IllegalArgumentException(
					"放映日期必須是明天或之後");
		}
	}

	// 判斷放映日期是否為今天或過去。
	private boolean isExpired(LocalDateTime ticketShowtime) {

		if (ticketShowtime == null) {
			return false;
		}

		LocalDate showtimeDate = ticketShowtime.toLocalDate();
		LocalDate today = LocalDate.now(TAIPEI_ZONE);

		return !showtimeDate.isAfter(today);
	}

	// 取得台灣時間明天的 00:00:00。
	// 資料庫使用 LocalDateTime 比較，
	// 但分界點確保今天所有時間都視為過期。
	private LocalDateTime getTomorrowStartOfDay() {

		return LocalDate.now(TAIPEI_ZONE)
				.plusDays(1)
				.atStartOfDay();
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

	// 初始化票券刊登者會員資料，供 Response 取得 usersName
	private void initializeListingUser(TicketListingBean listing) {

		UsersBean usersBean = listing.getUsersBean();

		if (usersBean != null) {
			Hibernate.initialize(usersBean);
		}
	}
}
