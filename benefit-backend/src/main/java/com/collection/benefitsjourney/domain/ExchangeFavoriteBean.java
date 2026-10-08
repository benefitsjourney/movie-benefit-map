package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "exchange_favorite", comment = "交換收藏表",
	uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_ExchangeFavorite_User_Listing",
            columnNames = {
                "users_id",
                "exchange_listing_id"
            }
        )
    })
public class ExchangeFavoriteBean {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "exchange_favorite_id", comment = "交換收藏表 ID")
	private Integer exchangeFavoriteId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "users_id", nullable = false)
	private UsersBean usersBean;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "exchange_listing_id", nullable = false)
	private ExchangeListingBean exchangeListingBean;

	@Column(name = "created_at", updatable = false, nullable = false, comment = "建立時間")
	@CreationTimestamp
	private LocalDateTime createdAt;

}
