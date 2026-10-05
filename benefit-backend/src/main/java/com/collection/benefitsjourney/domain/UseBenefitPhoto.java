package com.collection.benefitsjourney.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "use_benefit_photo")
@Getter
@Setter
public class UseBenefitPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "use_benefit_photo_id")
    private Integer useBenefitPhotoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "use_benefit_id", nullable = false)
    private UseBenefit useBenefit;

    @Column(name = "public_id", length = 200)
    private String publicId;

    @Column(name = "use_benefit_photo_url", length = 200)
    private String useBenefitPhotoUrl;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}