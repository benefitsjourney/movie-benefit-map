package com.collection.benefitsjourney.enums;

public class CinemaSort {

    public enum SortBy {
        CINEMA_ID("cinemaId"),
        CINEMA_NAME("cinemaName"),
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