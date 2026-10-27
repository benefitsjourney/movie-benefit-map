package com.collection.benefitsjourney;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BenefitsjourneyApplication {
	private static final Logger log = LoggerFactory.getLogger(BenefitsjourneyApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(BenefitsjourneyApplication.class, args);
		String url = context.getEnvironment().getProperty("spring.datasource.url", "");

		log.info("BenefitsJourney 啟動完成");
		log.info("目前連線資料庫：{}", describeDatabase(url));
		log.info("Swagger: http://localhost:8080/swagger-ui/index.html");
	}

	static String describeDatabase(String url) {
		String normalized = url == null ? "" : url.toLowerCase();
		if (normalized.startsWith("jdbc:sqlserver:")) {
			return "Microsoft SQL Server";
		}
		if (normalized.startsWith("jdbc:h2:")) {
			return "H2";
		}
		if (normalized.isBlank()) {
			return "未知（未設定 spring.datasource.url）";
		}
		return "未知";
	}
}
