package com.collection.benefitsjourney;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BenefitsjourneyApplication {
	private static final Logger log = LoggerFactory.getLogger(BenefitsjourneyApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BenefitsjourneyApplication.class, args);

		log.info("BenefitsJourney 啟動完成");
		log.info("Swagger: http://localhost:8080/swagger-ui/index.html");
	}
}
