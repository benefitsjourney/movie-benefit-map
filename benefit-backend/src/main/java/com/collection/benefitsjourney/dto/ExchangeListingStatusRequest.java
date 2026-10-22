package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於切換 ACTIVE CLOSED DELETED 狀態。
public class ExchangeListingStatusRequest {

    @NotBlank(message = "刊登狀態不可空白")
    private String status;
}
