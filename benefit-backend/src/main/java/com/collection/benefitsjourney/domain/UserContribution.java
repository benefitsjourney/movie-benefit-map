package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_contribution")
@Getter
@Setter
public class UserContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contribution_id")
    private Integer contributionId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "contribution_type", length = 32, nullable = false)
    private String contributionType;

    @Column(name = "reference_id", nullable = false)
    private Integer referenceId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}