package com.collection.benefitsjourney.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    // 回饋編號
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Integer feedbackId;

    // 提交回饋的會員
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UsersBean user;

    // 回饋類型
    @Column(name = "category", nullable = false, length = 30)
    private String category;

    // 回饋標題
    @Nationalized
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 回饋內容
    @Nationalized
    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    // PENDING / PROCESSING / COMPLETED
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    // 管理員回覆
    @Nationalized
    @Column(name = "admin_reply", length = 2000)
    private String adminReply;

    // 提交時間
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 最後更新時間
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}