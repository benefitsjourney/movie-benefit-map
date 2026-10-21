package com.collection.benefitsjourney.dto.CinemaMovie;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.MovieBean;

/** 前台電影探索卡片需要的欄位，含上架特典筆數 */
public record MovieExploreResponse(
        Integer movieId,
        String movieName,
        String imageUrl,
        LocalDateTime releaseDate,
        int benefitCount) {

    public static MovieExploreResponse from(MovieBean movie, int benefitCount) {
        return new MovieExploreResponse(
                movie.getMovieId(),
                movie.getMovieName(),
                movie.getImageUrl(),
                movie.getReleaseDate(),
                benefitCount);
    }
}
