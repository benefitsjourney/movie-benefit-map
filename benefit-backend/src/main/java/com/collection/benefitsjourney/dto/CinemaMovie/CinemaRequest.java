package com.collection.benefitsjourney.dto.CinemaMovie;

import java.math.BigDecimal;

import com.collection.benefitsjourney.enums.County;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CinemaRequest(

        @Size(max = 64, message = "來源ID長度不可超過64字元") String sourceId,

        @Size(max = 256, message = "影城名稱長度不可超過256字元") @NotBlank(groups = OnCreate.class, message = "影城名稱不能為空") String cinemaName,

        @NotNull(groups = OnCreate.class, message = "縣市不能為空") County county,

        @Size(max = 256, message = "地址長度不可超過256字元") String cinemaAddress,

        @DecimalMin(value = "-90", message = "緯度不可小於-90") @DecimalMax(value = "90", message = "緯度不可大於90") @Digits(integer = 3, fraction = 7, message = "緯度須為小數點前最多3位、小數點後最多7位") BigDecimal latitude,

        @DecimalMin(value = "-180", message = "經度不可小於-180") @DecimalMax(value = "180", message = "經度不可大於180") @Digits(integer = 3, fraction = 7, message = "經度須為小數點前最多3位、小數點後最多7位") BigDecimal longitude,

        @Size(max = 512, message = "官方網站長度不可超過512字元") String officialWebsiteUrl,

        Boolean isActive,

        Boolean isVisible) {

    /** 新增影城時才檢查必填欄位；修改時允許省略未變更欄位 */
    public interface OnCreate {
    }
}
