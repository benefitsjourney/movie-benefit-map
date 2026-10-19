package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "訊息內容不可為空")
    @Size(max = 2000, message = "訊息長度不可超過 2000 字")
    private String content;
}
