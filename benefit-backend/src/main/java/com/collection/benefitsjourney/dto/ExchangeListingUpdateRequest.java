package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於接收前端修改交換刊登的資料。
// 不允許透過此 DTO 修改持有紀錄的歸屬。
public class ExchangeListingUpdateRequest {

    @NotBlank(message = "交換刊登標題不可空白")
    @Size(max = 100, message = "標題最多 100 個字元")
    private String title;

    @Size(max = 500, message = "刊登說明最多 500 個字元")
    private String description;
}
