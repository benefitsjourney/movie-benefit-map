package com.collection.benefitsjourney.dto;

import com.collection.benefitsjourney.enums.BenefitInventoryStatus;

import jakarta.validation.constraints.NotNull;

/** 新增庫存回報 */
public record InventoryReportCreateRequest(
        @NotNull(message = "請選擇影城特典") Integer cinemaMovieBenefitId,
        @NotNull(message = "庫存狀態不能為空") BenefitInventoryStatus status) {
}

