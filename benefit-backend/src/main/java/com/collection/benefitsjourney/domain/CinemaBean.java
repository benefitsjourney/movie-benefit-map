package com.collection.benefitsjourney.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.collection.benefitsjourney.enums.County;

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
@Table(name = "cinema", comment = "影城表")
public class CinemaBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cinema_id", comment = "影城 ID")
    private Integer cinemaId;

    @Column(name = "cinema_name", nullable = false, length = 64, comment = "影城名稱")
    private String cinemaName;

    @Column(name = "address", length = 256, comment = "地址")
    private String address;

    @Column(name = "latitude", precision = 10, scale = 6, comment = "緯度")
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 6, comment = "經度")
    private BigDecimal longitude;

    @Column(name = "official_website_url", length = 512, comment = "影城官方網站連結")
    private String officialWebsiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "county", nullable = false, comment = "縣市")
    private County county;

    @Column(name = "is_visible", nullable = false, comment = "影城是否顯示")
    private Boolean isVisible = true;

    @Column(name = "is_active", nullable = false, comment = "影城是否營業／啟用")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, comment = "建立時間")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", comment = "資料最後更新時間")
    private LocalDateTime updatedAt;
}