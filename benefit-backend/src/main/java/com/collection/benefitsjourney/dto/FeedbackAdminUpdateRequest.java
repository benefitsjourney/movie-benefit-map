package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackAdminUpdateRequest {

    // PENDING / PROCESSING / COMPLETED
    @NotBlank(message = "請選擇處理狀態")
    private String status;

    @Size(max = 2000, message = "管理員回覆不可超過 2000 字")
    private String adminReply;
}