package com.collection.benefitsjourney.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
public class CinemaMovieBenefit {

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

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}