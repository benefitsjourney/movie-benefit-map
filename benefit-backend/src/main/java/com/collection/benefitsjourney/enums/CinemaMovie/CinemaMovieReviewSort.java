package com.collection.benefitsjourney.enums.CinemaMovie;

public class CinemaMovieReviewSort {

    public enum SortBy {
        CINEMA_MOVIE_REVIEW_ID("cinemaMovieReviewId"),
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
