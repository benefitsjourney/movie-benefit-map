package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotNull;

/** 新增持有紀錄：只要帶特典ID，使用者ID由後端從登入資訊取得 */
public record UseBenefitCreateRequest(
        @NotNull(message = "請選擇特典") Integer benefitId) {
}