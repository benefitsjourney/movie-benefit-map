package com.collection.benefitsjourney.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "reminder",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_reminder_user_type_target",
            columnNames = {
                "user_id",
                "reminder_type",
                "benefit_id",
                "movie_id"
            }
        )
    }
)
@Getter
@Setter
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Integer reminderId;

    @Column(name = "reminder_type", length = 16, nullable = false)
    private String reminderType;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "benefit_id")
    private Integer benefitId;

    @Column(name = "movie_id")
    private Integer movieId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}