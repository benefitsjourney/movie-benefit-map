package com.collection.benefitsjourney.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_report")
@Getter
@Setter
public class InventoryReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer reportId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "cinema_movie_benefit_id",
        nullable = false
    )
    private CinemaMovieBenefit cinemaMovieBenefit;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;
}