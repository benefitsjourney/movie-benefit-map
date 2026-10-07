package com.collection.benefitsjourney.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeListingRequest {

    private String title;

    private String description;

    private Integer useBenefitId;
}