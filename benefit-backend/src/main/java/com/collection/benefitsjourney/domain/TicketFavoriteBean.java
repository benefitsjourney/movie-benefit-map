package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ticket_favorite", comment = "轉讓收藏表")
public class TicketFavoriteBean {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ticket_favorite_id", comment = "收藏 ID")
	private Integer ticketFavoriteId;

	@Column(name = "user_id", nullable = false)
	private Integer userId;
	// @ManyToOne
	// // (fetch = FetchType.LAZY)
	// @JoinColumn(name = "users_id", nullable = false)
	// private UsersBean usersBean;

	@ManyToOne
	// (fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_listing_id", nullable = false)
	private TicketListingBean ticketListingBean;

	@Column(name = "created_at", updatable = false, comment = "建立時間")
	@CreationTimestamp
	private LocalDateTime createdAt;
}
