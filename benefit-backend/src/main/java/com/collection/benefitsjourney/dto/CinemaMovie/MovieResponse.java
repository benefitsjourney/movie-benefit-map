package com.collection.benefitsjourney.dto.CinemaMovie;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.MovieBean;

public record MovieResponse(
        Integer movieId,
        String sourceId,
        String sourceUrl,
        String movieName,
        String imageUrl,
        Boolean isVisible,
        LocalDateTime releaseDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MovieResponse from(MovieBean movie) {
        return new MovieResponse(
                movie.getMovieId(),
                movie.getSourceId(),
                movie.getSourceUrl(),
                movie.getMovieName(),
                movie.getImageUrl(),
                movie.getIsVisible(),
                movie.getReleaseDate(),
                movie.getCreatedAt(),
                movie.getUpdatedAt());
    }
}
