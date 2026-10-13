package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

	@Column(name = "title", columnDefinition = "NVARCHAR(100)", nullable = false, comment = "票劵標題")
	private String title;

	@Column(name = "description",columnDefinition = "NVARCHAR(500)", comment = "刊登說明")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "users_id", nullable = false, comment = "賣家會員 ID")
	private UsersBean usersBean;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cinema_id", nullable = false, comment = "影城 ID")
	private CinemaBean cinemaBean;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "movie_id", nullable = false, comment = "電影 ID")
	private MovieBean movieBean;

	@Column(name = "ticket_showtime", nullable = false, comment = "電影時間")
	private LocalDateTime ticketShowtime;

//	開啟(ACTIVE)  = 現在正在刊登 / 隱藏(HIDDEN)  = 暫時不刊登 / 關閉(CLOSED )  = 刊登已經結束 / 刪除(DELETED) = 使用者不要這筆刊登
	@Column(name = "status",columnDefinition = "NVARCHAR(20)", nullable = false, comment = "刊登狀態")
	private String status;

	@Column(name = "created_at", updatable = false, nullable = false, comment = "建立時間")
	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(name = "updated_at", comment = "更新時間")
	@UpdateTimestamp
	private LocalDateTime updatedAt;

}
