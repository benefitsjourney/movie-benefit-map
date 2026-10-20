package com.collection.benefitsjourney.enums;

public class CrawlerLogSort {

    public enum SortBy {
        CRAWLER_LOG_ID("crawlerLogId"),
        CRAWLER_TYPE("crawlerType"),
        CRAWLER_STATUS("crawlerStatus"),
        EXECUTED_AT("executedAt"),
        FINISHED_AT("finishedAt");

        private final String property;

        SortBy(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }
}
