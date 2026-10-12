package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於回傳前端顯示的交換刊登資料。
public class ExchangeListingRequest {

    @NotBlank(message = "交換刊登標題不可空白")
    @Size(max = 100, message = "標題最多 100 個字元")
    private String title;

    @Size(max = 500, message = "刊登說明最多 500 個字元")
    private String description;

    @NotNull(message = "請選擇持有紀錄")
    private Integer useBenefitId;
}