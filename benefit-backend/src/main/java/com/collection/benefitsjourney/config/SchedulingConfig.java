package com.collection.benefitsjourney.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 自動啟用排程，票劵日期檢查用
// 與 TicketListingExpirationScheduler.java .配合，將 過去(含)今天的票劵查出來。
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
