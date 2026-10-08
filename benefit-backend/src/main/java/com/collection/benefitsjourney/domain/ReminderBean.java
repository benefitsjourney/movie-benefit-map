package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "reminder",
    // 分別限制同一使用者不能重複追蹤相同的特典或電影
    indexes = {
        @Index(
            name = "uk_reminder_user_type_benefit",
            columnList = "user_id, reminder_type, benefit_id",
            unique = true
        ),
        @Index(
            name = "uk_reminder_user_type_movie",
            columnList = "user_id, reminder_type, movie_id",
            unique = true
        )
    },
    // 是追蹤特典還是電影的提醒
    check = {
        @CheckConstraint(
            name = "ck_reminder_target",
            constraint = "(reminder_type = 'BENEFIT' AND benefit_id IS NOT NULL AND movie_id IS NULL) "
                       + "OR (reminder_type = 'MOVIE' AND movie_id IS NOT NULL AND benefit_id IS NULL)"
        )
    }
)
@Getter
@Setter
public class ReminderBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Integer reminderId;

    // BENEFIT或MOVIE
    @Column(name = "reminder_type", length = 16, nullable = false)
    private String reminderType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UsersBean user;

    // 追蹤特典時有值，特典刪除時提醒一併刪除
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "benefit_id")
    private BenefitBean benefit;

    // 追蹤電影時有值，電影刪除時提醒一併刪除
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "movie_id")
    private MovieBean movie;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}