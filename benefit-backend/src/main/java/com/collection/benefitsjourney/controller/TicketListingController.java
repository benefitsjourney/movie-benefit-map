package com.collection.benefitsjourney.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.service.TicketListingService;

@RestController
@RequestMapping("/ticket-listings")
public class TicketListingController {
	private TicketListingService ticketListingService;
}
