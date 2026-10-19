package com.collection.benefitsjourney.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.ReviewStatus;

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
@Table(name = "cinema_movie_review", comment = "爬蟲審核表")
public class CinemaMovieReviewBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cinema_movie_review_id", comment = "ReviewID")
    private Integer cinemaMovieReviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawler_type", nullable = false, length = 32, comment = "爬蟲類型")
    private CinemaResourceType crawlerType;

    @Column(name = "entity_id", nullable = false, comment = "由爬蟲類型對應的資料表ID，例如movie_id / cinema_id")
    private Integer entityId;

    @Column(name = "field_name", nullable = false, length = 64, comment = "被人工修改的欄位名稱，例如 movie_name、address")
    private String fieldName;

    @Column(name = "crawler_value", length = 512, comment = "爬蟲最新抓到的值")
    private String crawlerValue;

    @Column(name = "previous_value", length = 512, comment = "覆蓋前原本的資料值")
    private String previousValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 32, comment = "審查狀態")
    private ReviewStatus reviewStatus;

    @Column(name = "reviewed_at", comment = "審核資料時間")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, comment = "建立Review時間")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", comment = "最後更新時間")
    private LocalDateTime updatedAt;
}
