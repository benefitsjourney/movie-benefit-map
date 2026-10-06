package com.collection.benefitsjourney.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.service.ExchangeFavoriteService;

@RestController
@RequestMapping("/exchange-favorites")
public class ExchangeFavoriteController {

	private ExchangeFavoriteService exchangeFavoriteService; 
}
