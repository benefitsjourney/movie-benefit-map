package com.collection.benefitsjourney.dto;

import com.collection.benefitsjourney.enums.BenefitStatus;

import jakarta.validation.constraints.NotNull;

/** 上下架特典：APPROVED（已上架）／EXPIRED（已下架） */
public record BenefitStatusRequest(
        @NotNull(message = "特典狀態不能為空") BenefitStatus status) {
}
