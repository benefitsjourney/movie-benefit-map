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
import lombok.Setter;

@Entity
@Table(name = "benefit")
@Getter
@Setter
public class BenefitBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_id")
    private Integer benefitId;

    @Column(name = "public_id", length = 200)
    private String publicId;

    @Column(name = "benefit_name", length = 100, nullable = false)
    private String benefitName;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}