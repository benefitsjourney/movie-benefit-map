package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UseBenefitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 類別預設使用唯讀交易。
// 新增、修改或刪除資料的方法，另外使用 @Transactional。
public class ExchangeListingService {

	private final ExchangeListingRepository exchangeListingRepository;
	private final UseBenefitRepository useBenefitRepository;

	// 新增交換刊登
	@Transactional
	public ExchangeListingBean createListing(Integer usersId, String title, String description, Integer useBenefitId) {

		// 1. 找到 UseBenefit，確認持有紀錄存在
		UseBenefitBean useBenefitBean = useBenefitRepository.findById(useBenefitId)
				.orElseThrow(() -> new IllegalArgumentException("找不到持有紀錄"));

		// 2. 確認這筆 UseBenefit 是目前登入會員的
		UsersBean usersBean = useBenefitBean.getUser();

		if (usersBean == null
				|| !usersId.equals(usersBean.getUsersId())) {
			throw new IllegalArgumentException(
					"無法使用其他會員的持有紀錄");
		}

		// 3. 確認這筆 UseBenefit 尚未被刊登
		boolean alreadyListed = exchangeListingRepository
				.findByUseBenefitBean(useBenefitBean)
				.isPresent();

		if (alreadyListed) {
			throw new IllegalArgumentException(
					"這筆持有紀錄已經刊登過");
		}

		// 4. 建立交換刊登
		ExchangeListingBean listing = new ExchangeListingBean();

		listing.setTitle(title);
		listing.setDescription(description);
		// 新增時預設 ACTIVE
		listing.setStatus("ACTIVE");
		listing.setUseBenefitBean(useBenefitBean);

		// 5. 儲存
		return exchangeListingRepository.save(listing);
	}

	// 查詢公開的交換刊登，只顯示 ACTIVE
	public List<ExchangeListingBean> findActiveListings() {
		return exchangeListingRepository.findByStatus("ACTIVE");
	}

	// 內部查詢：依 ID 查詢，不限制狀態，用於會員自己的修改及狀態操作
	public ExchangeListingBean findById(Integer exchangeListingId) {
		return exchangeListingRepository
				.findById(exchangeListingId)
				.orElseThrow(() -> new IllegalArgumentException("找不到交換刊登"));
	}

	// 公開查詢單筆交換刊登，只允許 ACTIVE
	public ExchangeListingBean findActiveById(Integer exchangeListingId) {
		return exchangeListingRepository
				.findByExchangeListingIdAndStatus(exchangeListingId, "ACTIVE")
				.orElseThrow(() -> new IllegalArgumentException("找不到可公開查看的交換刊登"));
	}

	// 查詢自己的交換刊登
	public List<ExchangeListingBean> findMyListings(Integer usersId) {
		return exchangeListingRepository.findByUseBenefitBean_User_UsersId(usersId);
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
		return exchangeListingRepository.save(listing);
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

		// 3. 檢查狀態是否合法
		if (!"ACTIVE".equals(status)
				&& !"HIDDEN".equals(status)
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
		return exchangeListingRepository.save(listing);
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
