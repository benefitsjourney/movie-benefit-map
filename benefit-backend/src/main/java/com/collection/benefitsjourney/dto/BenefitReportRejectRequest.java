package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.Size;

/** 駁回特典回報，駁回理由會給使用者查看 */
public record BenefitReportRejectRequest(
        @Size(max = 100, message = "審核備註長度不可超過100字元") String reviewNote) {
}
