package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "ticket_listing", comment = "票券刊登表")
public class TicketListingBean {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ticket_listing_id", comment = "票券刊登編號 ID")
	private Integer ticketListingId;

	@Column(name = "title", length = 100, nullable = false, comment = "票劵標題")
	private String title;

	@Column(name = "description", length = 500, comment = "刊登說明")
	private String description;

	@Column(name = "user_id", nullable = false)
	private Integer userId;
	// @ManyToOne
	// // (fetch = FetchType.LAZY)
	// @JoinColumn(name = "users_id", nullable = false, comment = "賣家會員 ID")
	// private UsersBean usersBean;

	@ManyToOne
	// (fetch = FetchType.LAZY)
	@JoinColumn(name = "cinema_id", nullable = false, comment = "影城 ID")
	private CinemaBean cinemaBean;

	@Column(name = "movie_id", nullable = false)
	private Integer movieId;
	// @ManyToOne
	// // (fetch = FetchType.LAZY)
	// @JoinColumn(name = "movie_id", nullable = false, comment = "電影 ID")
	// private MovieBean movieBean;

	@Column(name = "ticket_showtime", nullable = false, comment = "電影時間")
	private LocalDateTime ticketShowtime;

	// ACTIVE = 開啟/ CLOSED = 關閉
	@Column(name = "status", length = 20, nullable = false, comment = "刊登狀態")
	private String status;

	@Column(name = "created_at", updatable = false, comment = "建立時間")
	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(name = "updated_at", comment = "更新時間")
	@UpdateTimestamp
	private LocalDateTime updatedAt;

}
