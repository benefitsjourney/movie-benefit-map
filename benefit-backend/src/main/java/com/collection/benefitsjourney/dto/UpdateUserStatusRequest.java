package com.collection.benefitsjourney.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 

public class UpdateUserStatusRequest {

    @NotBlank
    private String status;

}
