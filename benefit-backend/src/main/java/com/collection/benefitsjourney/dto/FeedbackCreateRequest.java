package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackCreateRequest {

    // FEATURE_SUGGESTION / BUG_REPORT / QUESTION / OTHER
    @NotBlank(message = "請選擇回饋類型")
    private String category;

    @NotBlank(message = "請輸入回饋標題")
    @Size(max = 100, message = "標題不可超過 100 字")
    private String title;

    @NotBlank(message = "請輸入回饋內容")
    @Size(max = 2000, message = "回饋內容不可超過 2000 字")
    private String content;
}