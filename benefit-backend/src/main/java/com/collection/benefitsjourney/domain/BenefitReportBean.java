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
import lombok.Setter;

@Entity
@Table(name = "benefit_report")
@Getter
@Setter
public class BenefitReportBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_report_id")
    private Integer benefitReportId;

    // 放圖片用
    @Column(name = "public_id", length = 200)
    private String publicId;

    // 爬蟲建立的回報沒有使用者，所以可為空
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UsersBean user;

    // 資料庫查不到影城時為空，改存customCinemaName
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private CinemaBean cinema;

    // 資料庫查不到電影時為空，改存customMovieName
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private MovieBean movie;

    // 審核通過後才會對應到正式特典
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id")
    private BenefitBean benefit;

    @Column(name = "benefit_name", length = 100)
    private String benefitName;

    @Column(name = "custom_cinema_name", length = 100)
    private String customCinemaName;

    @Column(name = "custom_movie_name", length = 100)
    private String customMovieName;

    @Column(name = "is_member_only")
    private Boolean isMemberOnly;

    @Column(name = "benefit_type", length = 32)
    private String benefitType;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "official_image_url", length = 200)
    private String officialImageUrl;

    @Column(name = "source_url", length = 300)
    private String sourceUrl;

    @Column(name = "review_note", length = 100)
    private String reviewNote;
    
    // 爬蟲匯入時比對到的影城ID，用逗號隔開（例如 1,2,3）。「重新比對」補齊電影後，靠它知道要綁哪些影城
    @Column(name = "matched_cinema_ids", length = 600)
    private String matchedCinemaIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
