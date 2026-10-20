package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "crawler_log", comment = "爬蟲紀錄")
public class CrawlerLogBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crawler_log_id", comment = "爬蟲紀錄ID")
    private Integer crawlerLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawler_type", nullable = false, length = 32, comment = "爬蟲類型")
    private CrawlerType crawlerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawler_status", nullable = false, length = 16, comment = "執行狀態")
    private CrawlerStatus crawlerStatus;

    @Column(name = "fetched_count", nullable = false, comment = "本次抓到的資料總數")
    private Integer fetchedCount;

    @Column(name = "created_count", nullable = false, comment = "本次新增的資料總數")
    private Integer createdCount;

    @Column(name = "updated_count", nullable = false, comment = "本次更新的資料總數")
    private Integer updatedCount;

    @Column(name = "skipped_count", nullable = false, comment = "本次略過的資料總數")
    private Integer skippedCount;

    @Column(name = "pending_review_count", nullable = false, comment = "本次納入審核的資料總數")
    private Integer pendingReviewCount;

    @Column(name = "message", length = 1024, comment = "執行結果或錯誤訊息")
    private String message;

    @Column(name = "executed_at", nullable = false, comment = "執行起始時間")
    private LocalDateTime executedAt;

    @Column(name = "finished_at", nullable = false, comment = "爬蟲結束時間")
    private LocalDateTime finishedAt;
}
