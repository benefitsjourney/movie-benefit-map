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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "movie", comment = "電影表")
public class MovieBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id", comment = "電影 ID")
    private Integer movieId;

    @Column(name = "source_id", unique = true, length = 64, comment = "電影辨認ID")
    private String sourceId;

    @Column(name = "movie_name", nullable = false, length = 256, comment = "電影名稱")
    private String movieName;

    @Column(name = "image_url", length = 512, comment = "電影圖片")
    private String imageUrl;

    @Column(name = "is_visible", nullable = false, comment = "電影是否顯示")
    private Boolean isVisible = true;

    @Column(name = "release_date", comment = "上映日期")
    private LocalDateTime releaseDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, comment = "資料建立時間")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", comment = "資料最後更新時間")
    private LocalDateTime updatedAt;
}
