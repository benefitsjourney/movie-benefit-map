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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "use_benefit_photo")
@Getter
@Setter
public class UseBenefitPhotoBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "use_benefit_photo_id")
    private Integer useBenefitPhotoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "use_benefit_id", nullable = false)
    private UseBenefitBean useBenefit;

    @Column(name = "public_id", length = 200)
    private String publicId;

    @Column(name = "use_benefit_photo_url", length = 200)
    private String useBenefitPhotoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}