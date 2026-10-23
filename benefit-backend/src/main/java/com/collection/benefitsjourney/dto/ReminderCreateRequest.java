package com.collection.benefitsjourney.dto;

import com.collection.benefitsjourney.enums.ReminderType;

import jakarta.validation.constraints.NotNull;

/**
 * 新增追蹤
 * 追蹤特典：{"reminderType":"BENEFIT","benefitId":1}
 * 追蹤電影：{"reminderType":"MOVIE","movieId":1}
 * 使用者ID由後端從登入資訊取得
 */
public record ReminderCreateRequest(
        @NotNull(message = "請選擇追蹤類型") ReminderType reminderType,
        Integer benefitId,
        Integer movieId) {
}
