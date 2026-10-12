package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於接收前端修改票券刊登的資料。
public class TicketListingUpdateRequest {
    @NotBlank(message = "票券刊登標題不可空白")
    @Size(max = 100, message = "標題最多 100 個字元")
    private String title;

    @Size(max = 500, message = "刊登說明最多 500 個字元")
    private String description;

    @NotNull(message = "請選擇影城")
    private Integer cinemaId;

    @NotNull(message = "請選擇電影")
    private Integer movieId;

    @NotNull(message = "請填寫票券場次時間")
    private LocalDateTime ticketShowtime;
}
