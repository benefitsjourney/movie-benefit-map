package com.collection.benefitsjourney.enums.CinemaMovie;

public class CinemaSort {

    public enum SortBy {
        CINEMA_ID("cinemaId"),
        COUNTY("county"),
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
