package com.collection.benefitsjourney.enums.CinemaMovie;

public class CinemaMovieCrawlerReviewSort {

    public enum SortBy {
        CRAWLER_REVIEW_ID("crawlerReviewId"),
        CRAWLER_TYPE("crawlerType"),
        ENTITY_ID("entityId"),
        REVIEW_STATUS("reviewStatus"),
        REVIEWED_AT("reviewedAt"),
        CREATED_AT("createdAt"),
        UPDATED_AT("updatedAt");

        private final String property;

        SortBy(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }
}
