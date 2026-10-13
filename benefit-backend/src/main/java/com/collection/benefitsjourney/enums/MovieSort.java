package com.collection.benefitsjourney.enums;

public class MovieSort {

    public enum SortBy {
        MOVIE_ID("movieId"),
        RELEASE_DATE("releaseDate"),
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
