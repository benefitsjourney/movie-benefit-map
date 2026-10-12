package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketListingStatusRequest {
    @NotBlank(message = "刊登狀態不可空白")
    private String status;
}
