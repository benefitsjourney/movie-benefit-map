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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "cinema_movie_benefit",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cinema_movie_benefit",
            columnNames = {
                "cinema_id",
                "movie_id",
                "benefit_id"
            }
        )
    }
)
@Getter
@Setter
public class CinemaMovieBenefitBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cinema_movie_benefit_id")
    private Integer cinemaMovieBenefitId;

    @Column(name = "cinema_id", nullable = false)
    private Integer cinemaId;

    @Column(name = "movie_id", nullable = false)
    private Integer movieId;

    @Column(name = "benefit_id", nullable = false)
    private Integer benefitId;

    @Column(name = "benefit_inventory_status")
    private Integer benefitInventoryStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}