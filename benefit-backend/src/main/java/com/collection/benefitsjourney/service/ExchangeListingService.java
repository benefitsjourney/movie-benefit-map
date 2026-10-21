package com.collection.benefitsjourney.service;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UseBenefitRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 類別預設使用唯讀交易。
// 新增、修改或刪除資料的方法，另外使用 @Transactional。
public class ExchangeListingService {

	private final ExchangeListingRepository exchangeListingRepository;
	private final UseBenefitRepository useBenefitRepository;
	private final UsersRepository usersRepository;

	// 新增交換刊登
	@Transactional
	public ExchangeListingBean createListing(Integer usersId, String title, String description, Integer useBenefitId) {

		// 1. 找到 UseBenefit，確認持有紀錄存在
		UseBenefitBean useBenefitBean = useBenefitRepository.findById(useBenefitId)
				.orElseThrow(() -> new IllegalArgumentException("找不到持有紀錄"));

		// 2. 已軟刪除的持有紀錄不能拿來刊登
		if (useBenefitBean.getDeletedAt() != null) {
			throw new IllegalArgumentException("找不到持有紀錄");
		}

		// 3. 確認這筆 UseBenefit 是目前登入會員的
		UsersBean usersBean = useBenefitBean.getUser();

		if (usersBean == null
				|| !usersId.equals(usersBean.getUsersId())) {
			throw new IllegalArgumentException(
					"無法使用其他會員的持有紀錄");
		}

		// 4. 確認這筆 UseBenefit 尚未被刊登
		boolean alreadyListed = exchangeListingRepository
				.findByUseBenefitBean(useBenefitBean)
				.isPresent();

		if (alreadyListed) {
			throw new IllegalArgumentException(
					"這筆持有紀錄已經刊登過");
		}

		// 5. 建立交換刊登
		ExchangeListingBean listing = new ExchangeListingBean();

		listing.setTitle(title);
		listing.setDescription(description);
		// 新增時預設 ACTIVE
		listing.setStatus("ACTIVE");
		listing.setUseBenefitBean(useBenefitBean);

		// 6. 儲存
		ExchangeListingBean savedListing = exchangeListingRepository.save(listing);

		// 7. 預先載入回傳所需的特典及照片資料
		initializeListingDetails(savedListing);

		return savedListing;
	}

	// 查詢公開交換刊登
	public List<ExchangeListingBean> findActiveListings() {
		return findActiveListings(null, null, null, null);
	}

	// 查詢公開交換刊登，可依四項文字條件查詢
	public List<ExchangeListingBean> findActiveListings(
			String title,
			String usersName,
			String benefitName,
			String benefitType) {

		// 空白字串視為未輸入條件
		title = normalizeKeyword(title);
		usersName = normalizeKeyword(usersName);
		benefitName = normalizeKeyword(benefitName);
		benefitType = normalizeKeyword(benefitType);

		// Repository 會處理文字條件 OR，並且只查詢 ACTIVE
		List<ExchangeListingBean> listings = exchangeListingRepository
				.searchActiveListings(
						"ACTIVE",
						title,
						usersName,
						benefitName,
						benefitType);

		// 預先載入每筆刊登的特典及照片資料
		listings.forEach(this::initializeListingDetails);

		return listings;
	}

	// 將空白查詢轉為 null
	private String normalizeKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}

		return keyword.trim();
	}

	// 查詢指定 ACTIVE 會員的公開交換刊登。不存在或非 ACTIVE 會員一律不公開。
	public List<ExchangeListingBean> findActiveListingsByMember(Integer usersId) {

		UsersBean usersBean = usersRepository.findById(usersId)
				.orElseThrow(() -> new IllegalArgumentException("找不到此會員"));

		if (!"ACTIVE".equals(usersBean.getStatus())) {
			throw new IllegalArgumentException("找不到此會員");
		}

		List<ExchangeListingBean> listings = exchangeListingRepository
				.findByUseBenefitBean_User_UsersIdAndStatus(usersId, "ACTIVE");

		listings.forEach(this::initializeListingDetails);
		return listings;
	}

	// 依 ID 查詢，不限制狀態，供會員自己的修改及狀態操作使用
	public ExchangeListingBean findById(Integer exchangeListingId) {
		return exchangeListingRepository
				.findById(exchangeListingId)
				.orElseThrow(() -> new IllegalArgumentException("找不到交換刊登"));
	}

	// 公開查詢單筆交換刊登，只允許 ACTIVE
	public ExchangeListingBean findActiveById(Integer exchangeListingId) {
		ExchangeListingBean listing = exchangeListingRepository
				.findByExchangeListingIdAndStatus(exchangeListingId, "ACTIVE")
				.orElseThrow(() -> new IllegalArgumentException(
						"找不到可公開查看的交換刊登"));

		// 預先載入回傳所需的特典及照片資料
		initializeListingDetails(listing);

		return listing;
	}

	// 查詢自己的交換刊登
	public List<ExchangeListingBean> findMyListings(Integer usersId) {
		List<ExchangeListingBean> listings = exchangeListingRepository
				.findByUseBenefitBean_User_UsersId(usersId);

		// 預先載入每筆刊登的特典及照片資料
		listings.forEach(this::initializeListingDetails);

		return listings;
	}

	// 修改自己的刊登內容
	@Transactional
	public ExchangeListingBean updateListing(
			Integer usersId,
			Integer exchangeListingId,
			String title,
			String description) {

		// 1. 找刊登
		ExchangeListingBean listing = findById(exchangeListingId);

		// 2. 確認是不是自己的
		checkOwner(usersId, listing);

		// 3. DELETED 不可以修改
		if ("DELETED".equals(listing.getStatus())) {
			throw new IllegalArgumentException(
					"已刪除的刊登不能修改");
		}

		// 4. 修改內容
		listing.setTitle(title);
		listing.setDescription(description);

		// 5. 儲存
		ExchangeListingBean savedListing = exchangeListingRepository.save(listing);

		// 6. 預先載入回傳所需的特典及照片資料
		initializeListingDetails(savedListing);

		return savedListing;
	}

	// 修改自己的刊登狀態
	@Transactional
	public ExchangeListingBean updateStatus(
			Integer usersId,
			Integer exchangeListingId,
			String status) {

		// 1. 找刊登
		ExchangeListingBean listing = findById(exchangeListingId);

		// 2. 確認是不是自己的
		checkOwner(usersId, listing);

		// 3. 檢查狀態是否合法，只允許 ACTIVE、CLOSED、DELETED
		if (!"ACTIVE".equals(status)
				&& !"CLOSED".equals(status)
				&& !"DELETED".equals(status)) {

			throw new IllegalArgumentException(
					"無效的交換刊登狀態");
		}

		// 4. DELETED 之後不能再修改及重新開啟
		if ("DELETED".equals(listing.getStatus())
				&& !"DELETED".equals(status)) {

			throw new IllegalArgumentException(
					"已刪除的刊登不能重新開啟");
		}

		// 5. 修改狀態
		listing.setStatus(status);

		// 6. 儲存
		ExchangeListingBean savedListing = exchangeListingRepository.save(listing);

		// 7. 預先載入回傳所需的特典及照片資料
		initializeListingDetails(savedListing);

		return savedListing;
	}

	// 預先載入交換刊登回傳所需的特典與照片資料
	// 確保 Service 交易結束後，Controller 轉換 Response 時仍可取得資料
	private void initializeListingDetails(ExchangeListingBean listing) {

		UseBenefitBean useBenefit = listing.getUseBenefitBean();

		if (useBenefit == null) {
			return;
		}

		Hibernate.initialize(useBenefit);

		// 載入持有特典的會員，供 Response 取得 usersName
		UsersBean usersBean = useBenefit.getUser();

		if (usersBean != null) {
			Hibernate.initialize(usersBean);
		}

		// 載入特典基本資料
		if (useBenefit.getBenefit() != null) {
			Hibernate.initialize(useBenefit.getBenefit());
		}

		// 載入持有特典的照片清單
		// 沒有照片時，空清單仍然可以正常使用
		if (useBenefit.getPhotos() != null) {
			Hibernate.initialize(useBenefit.getPhotos());

			useBenefit.getPhotos().forEach(
					Hibernate::initialize);
		}
	}

	// 確認刊登是否為目前會員所有
	private void checkOwner(
			Integer usersId,
			ExchangeListingBean listing) {

		UseBenefitBean useBenefit = listing.getUseBenefitBean();

		if (useBenefit == null
				|| useBenefit.getUser() == null
				|| !usersId.equals(
						useBenefit.getUser().getUsersId())) {

			throw new IllegalArgumentException(
					"無權操作其他會員的交換刊登");
		}
	}

}
