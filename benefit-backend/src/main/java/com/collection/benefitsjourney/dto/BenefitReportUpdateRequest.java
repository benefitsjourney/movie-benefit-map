package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.enums.BenefitType;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 管理員修改待審核特典回報。
 * 欄位皆可省略，只更新有帶的欄位。
 * 不開放修改status，狀態只走approve與reject。
 */
public record BenefitReportUpdateRequest(

        Integer cinemaId,

        Integer movieId,

        @Size(max = 100, message = "特典名稱長度不可超過100字元") String benefitName,

        @Size(max = 100, message = "自訂影城名稱長度不可超過100字元") String customCinemaName,

        @Size(max = 100, message = "自訂電影名稱長度不可超過100字元") String customMovieName,

        Boolean isMemberOnly,

        BenefitType benefitType,

        LocalDateTime startDate,

        LocalDateTime endDate,

        @Size(max = 200, message = "官方圖片網址長度不可超過200字元") String officialImageUrl,

        @Size(max = 300, message = "來源網址長度不可超過300字元") String sourceUrl,

        @Size(max = 100, message = "審核備註長度不可超過100字元") String reviewNote,
        
        // 審核時勾選的影城：只儲存不核准時先記在matchedCinemaIds，下次打開會預先勾好
        List<Integer> cinemaIds,

        // 管理員換圖時跟officialImageUrl一起帶
        @Size(max = 200, message = "圖片publicId長度不可超過200字元") String publicId) {
}