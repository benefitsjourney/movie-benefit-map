package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "use_benefit")
@Getter
@Setter
public class UseBenefitBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "use_benefit_id")
    private Integer useBenefitId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "benefit_id", nullable = false)
    private Integer benefitId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(
        mappedBy = "useBenefit",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<UseBenefitPhotoBean> photos = new ArrayList<>();
}