
package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.enums.BenefitType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 使用者回報缺漏特典。
 * 電影、影城都是有id用id，沒有就填自訂名稱。
 */
public record BenefitReportCreateRequest(

        // 資料庫有的電影（從電影清單選）
        Integer movieId,

        // 資料庫沒有的電影才填
        @Size(max = 100, message = "電影名稱長度不可超過100字元")
        String customMovieName,

        // 資料庫有的影城（從影城清單選）
        Integer cinemaId,

        // 資料庫沒有的影城才填
        @Size(max = 100, message = "影城名稱長度不可超過100字元")
        String customCinemaName,

        // 特典名稱
        @NotBlank(message = "特典名稱不能為空")
        @Size(max = 100, message = "特典名稱長度不可超過100字元")
        String benefitName,

        // 是否為會員限定（可先不填，由管理員審核時補齊）
        Boolean isMemberOnly,

        // 特典類型（可先不填，由管理員審核時補齊）
        BenefitType benefitType,

        // 開始領取日期（可先不填，由管理員審核時補齊）
        LocalDateTime startDate,

        // 結束領取日期（可先不填，由管理員審核時補齊）
        LocalDateTime endDate,

        // 特典照片（前端上傳到Cloudinary後傳入網址與publicId）
        @Size(max = 200, message = "圖片網址長度不可超過200字元")
        String officialImageUrl,

        @Size(max = 200, message = "圖片publicId長度不可超過200字元")
        String publicId,

        // 資料來源網址
        @Size(max = 300, message = "來源網址長度不可超過300字元")
        String sourceUrl

) {
}
