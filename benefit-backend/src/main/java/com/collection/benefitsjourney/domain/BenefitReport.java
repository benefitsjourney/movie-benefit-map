package com.collection.benefitsjourney.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "benefit_report")
@Getter
@Setter
public class BenefitReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_report_id")
    private Integer benefitReportId;
    
    // 放圖片用
    @Column(name = "public_id", length = 200)
    private String publicId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "cinema_id")
    private Integer cinemaId;

    @Column(name = "movie_id")
    private Integer movieId;

    @Column(name = "benefit_id")
    private Integer benefitId;
    
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

    @Column(name = "status", length = 16)
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

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}