package com.collection.benefitsjourney.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.service.TicketFavoriteService;

@RestController
@RequestMapping("/ticket-favorites")
public class TicketFavoriteController {
	private TicketFavoriteService ticketFavoriteService;
}
