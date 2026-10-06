package com.collection.benefitsjourney.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.service.ExchangeListingService;

@RestController
@RequestMapping("/exchange-listings")
public class ExchangeListingController {

	private ExchangeListingService exchangeListingService;
}
