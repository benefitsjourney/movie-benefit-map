package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.conflict;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.dto.MovieRequest;
import com.collection.benefitsjourney.dto.MovieResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.MovieSort;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.LikeKeywords;
import com.collection.benefitsjourney.repository.MovieRepository;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /** 取得所有電影資料 */
    public PageResponse<MovieResponse> findAll(
            String keyword,
            Boolean isVisible,
            int page,
            int size,
            MovieSort.SortBy orderBy,
            SortOrder sort) {

        Sort.Direction direction = sort == SortOrder.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(direction, orderBy.getProperty()));

        Page<MovieResponse> result = movieRepository.findAllByFilters(
                LikeKeywords.toLikeKeyword(keyword),
                isVisible,
                pageable)
                .map(MovieResponse::from);

        return PageResponse.from(result);
    }

    /** 取得指定id電影資料 */
    public MovieResponse findById(Integer id) {
        MovieBean movie = movieRepository.findById(id)
                .orElseThrow(() -> notFound("找不到電影 ID：" + id));

        return MovieResponse.from(movie);
    }

    /** 新增電影 */
    public MovieResponse createMovie(MovieRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.movieName() == null
                || request.movieName().isBlank()) {
            throw badRequest("電影名稱不可為空");
        }

        if (request.sourceId() != null
                && movieRepository.findBySourceId(request.sourceId()).isPresent()) {
            throw conflict("來源 ID 已存在：" + request.sourceId());
        }

        MovieBean movie = new MovieBean();

        movie.setSourceId(request.sourceId());
        movie.setMovieName(request.movieName());
        movie.setImageUrl(request.imageUrl());
        movie.setReleaseDate(request.releaseDate());

        movie.setIsVisible(
                request.isVisible() != null
                        ? request.isVisible()
                        : true);

        MovieBean savedMovie = movieRepository.save(movie);

        return MovieResponse.from(savedMovie);
    }

    /** 修改電影 */
    public MovieResponse updateMovie(Integer id, MovieRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.movieName() != null
                && request.movieName().isBlank()) {
            throw badRequest("電影名稱不可為空");
        }

        MovieBean movie = movieRepository.findById(id)
                .orElseThrow(() -> notFound("找不到電影 ID：" + id));

        if (request.sourceId() != null) {
            movieRepository.findBySourceId(request.sourceId())
                    .filter(existing -> !existing.getMovieId().equals(id))
                    .ifPresent(existing -> {
                        throw conflict(
                                "來源 ID 已存在：" + request.sourceId());
                    });

            movie.setSourceId(request.sourceId());
        }

        if (request.movieName() != null) {
            movie.setMovieName(request.movieName());
        }

        if (request.imageUrl() != null) {
            movie.setImageUrl(request.imageUrl());
        }

        if (request.releaseDate() != null) {
            movie.setReleaseDate(request.releaseDate());
        }

        if (request.isVisible() != null) {
            movie.setIsVisible(request.isVisible());
        }

        MovieBean savedMovie = movieRepository.save(movie);

        return MovieResponse.from(savedMovie);
    }

    /** 根據來源ID取得電影 */
    public Optional<MovieResponse> findBySourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return Optional.empty();
        }

        return movieRepository.findBySourceId(sourceId)
                .map(MovieResponse::from);
    }
}
