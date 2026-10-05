package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "exchange_listing", comment = "交換刊登表")
public class ExchangeListingBean {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "exchange_listing_id", comment = "交換刊登編號 ID")
	private Integer exchangeListingId;

	@Column(name = "title", length = 100, nullable = false, comment = "刊登標題")
	private String title;

	@Column(name = "description", length = 500, comment = "刊登說明")
	private String description;

	// ACTIVE = 開啟
	// CLOSED = 關閉
	@Column(name = "status", length = 20, nullable = false, comment = "刊登狀態")
	private String status;

	// @OneToOne
	// @JoinColumn(
	// name = "use_benefit_id",
	// nullable = false,
	// unique = true //建議可以不加，因為要重複刊登
	// )
	// private UseBenefitBean useBenefitBean;

	@Column(name = "created_at", updatable = false, comment = "建立時間")
	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(name = "updated_at", comment = "更新時間")
	@UpdateTimestamp
	private LocalDateTime updatedAt;
}
