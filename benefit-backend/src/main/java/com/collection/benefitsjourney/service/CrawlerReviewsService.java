package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.CrawlerReviewBean;
import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.dto.CrawlerReviewRequest;
import com.collection.benefitsjourney.dto.CrawlerReviewResponse;
import com.collection.benefitsjourney.dto.MovieRequest;
import com.collection.benefitsjourney.dto.MovieResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.enums.CrawlerReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CrawlerReviewsRepository;

@Service
public class CrawlerReviewsService {
    public static final String CINEMA_NAME = "cinema_name";
    public static final String CINEMA_ADDRESS = "cinema_address";
    public static final String COUNTY = "county";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String OFFICIAL_WEBSITE_URL = "official_website_url";
    public static final String IS_ACTIVE = "is_active";
    public static final String IS_VISIBLE = "is_visible";
    /** 電影來源 ID 衝突審核 */
    public static final String SOURCE_ID = "sourceId";
    public static final String MOVIE_NAME = "movie_name";
    public static final String IMAGE_URL = "image_url";
    public static final String RELEASE_DATE = "release_date";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CrawlerReviewsRepository crawlerReviewsRepository;
    private final CinemaService cinemaService;
    private final MovieService movieService;

    public CrawlerReviewsService(
            CrawlerReviewsRepository crawlerReviewsRepository,
            CinemaService cinemaService,
            MovieService movieService) {
        this.crawlerReviewsRepository = crawlerReviewsRepository;
        this.cinemaService = cinemaService;
        this.movieService = movieService;
    }

    /** 依條件查詢爬蟲審核 */
    public PageResponse<CrawlerReviewResponse> findAll(
            CinemaResourceType crawlerType,
            ReviewStatus reviewStatus,
            int page,
            int size,
            CrawlerReviewSort.SortBy orderBy,
            SortOrder sort) {

        Sort.Direction direction = sort == SortOrder.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort order = Sort.by(direction, orderBy.getProperty());

        // 相同排序值以審核 ID 排序
        if (orderBy != CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID) {
            order = order.and(Sort.by(
                    Sort.Direction.ASC,
                    TypedPropertyPath.path((CrawlerReviewBean review) -> review.getCrawlerReviewId())));
        }

        PageRequest pageable = PageRequest.of(page - 1, size, order);

        Page<CrawlerReviewResponse> result = crawlerReviewsRepository.findAllByFilters(
                crawlerType,
                reviewStatus,
                pageable)
                .map(CrawlerReviewResponse::from);

        return PageResponse.from(result);
    }

    /** 提交差異：重複略過，待審核值變更則更新 */
    @Transactional
    public boolean submitCrawlerChange(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName,
            String crawlerValue,
            String previousValue) {
        return saveCrawlerChange(crawlerType, entityId, fieldName, crawlerValue, previousValue)
                .isPresent();
    }

    /** 新增待審核資料 */
    @Transactional
    public CrawlerReviewResponse createCrawlerReview(CrawlerReviewRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.crawlerType() == null) {
            throw badRequest("爬蟲類型不可為空");
        }

        if (request.entityId() == null) {
            throw badRequest("對應資料ID不可為空");
        }

        if (request.fieldName() == null
                || request.fieldName().isBlank()) {
            throw badRequest("欄位名稱不可為空");
        }

        if (request.reviewStatus() != null
                && request.reviewStatus() != ReviewStatus.PENDING) {
            throw badRequest("新增僅能建立待審核資料");
        }

        if (Objects.equals(request.crawlerValue(), request.previousValue())) {
            throw badRequest("爬蟲值與原值相同，無需新增");
        }

        return saveCrawlerChange(
                request.crawlerType(),
                request.entityId(),
                request.fieldName().trim(),
                request.crawlerValue(),
                request.previousValue())
                .map(CrawlerReviewResponse::from)
                .orElseThrow(() -> badRequest("已有相同差異的審核紀錄，不重複新增"));
    }

    /** 審核差異；電影來源ID衝突僅確認重複，不覆蓋來源 */
    @Transactional
    public CrawlerReviewResponse updateCrawlerReview(Integer id, CrawlerReviewRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.reviewStatus() == null) {
            throw badRequest("審查狀態不可為空");
        }

        CrawlerReviewBean review = crawlerReviewsRepository.findById(id)
                .orElseThrow(() -> notFound("找不到爬蟲審核ID：" + id));

        return switch (request.reviewStatus()) {
            case APPROVED -> CrawlerReviewResponse.from(approve(review));
            case REJECTED -> CrawlerReviewResponse.from(reject(review));
            case PENDING -> CrawlerReviewResponse.from(reopen(review));
        };
    }

    /** 刪除爬蟲審核 */
    @Transactional
    public void deleteCrawlerReview(Integer id) {
        if (!crawlerReviewsRepository.existsById(id)) {
            throw notFound("找不到爬蟲審核ID：" + id);
        }

        crawlerReviewsRepository.deleteById(id);
    }

    /** 統一欄位值格式 */
    public static String formatValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        if (value instanceof BigDecimal number) {
            return number.setScale(7, java.math.RoundingMode.HALF_UP).toPlainString();
        }

        if (value instanceof LocalDateTime dateTime) {
            return dateTime.withNano(0).format(DATE_TIME);
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return value.toString();
    }

    private Optional<CrawlerReviewBean> saveCrawlerChange(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName,
            String crawlerValue,
            String previousValue) {
        if (Objects.equals(crawlerValue, previousValue)) {
            return Optional.empty();
        }

        List<CrawlerReviewBean> reviews = crawlerReviewsRepository
                .findByCrawlerTypeAndEntityIdAndFieldName(crawlerType, entityId, fieldName);

        CrawlerReviewBean pending = reviews.stream()
                .filter(review -> review.getReviewStatus() == ReviewStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (pending != null) {
            if (sameDifference(pending, crawlerValue, previousValue)) {
                return Optional.empty();
            }

            pending.setCrawlerValue(crawlerValue);
            pending.setPreviousValue(previousValue);
            return Optional.of(crawlerReviewsRepository.save(pending));
        }

        // 已拒絕或已確認的來源衝突不再送審
        boolean duplicate = reviews.stream()
                .anyMatch(review -> (review.getReviewStatus() == ReviewStatus.REJECTED
                        || (crawlerType == CinemaResourceType.MOVIE
                                && SOURCE_ID.equals(fieldName)
                                && review.getReviewStatus() == ReviewStatus.APPROVED))
                        && sameDifference(review, crawlerValue, previousValue));

        if (duplicate) {
            return Optional.empty();
        }

        CrawlerReviewBean review = new CrawlerReviewBean();
        review.setCrawlerType(crawlerType);
        review.setEntityId(entityId);
        review.setFieldName(fieldName);
        review.setCrawlerValue(crawlerValue);
        review.setPreviousValue(previousValue);
        review.setReviewStatus(ReviewStatus.PENDING);

        return Optional.of(crawlerReviewsRepository.save(review));
    }

    private CrawlerReviewBean approve(CrawlerReviewBean review) {
        if (review.getReviewStatus() != ReviewStatus.PENDING) {
            throw badRequest("只有待審核資料可以接受");
        }

        // 來源衝突只確認重複，不修改電影
        if (isMovieSourceConflict(review)) {
            requireText(review.getCrawlerValue());
            movieService.findById(review.getEntityId()); // 確認被比對的既有電影仍存在
        } else {
            applyOfficialValue(review);
        }
        review.setReviewStatus(ReviewStatus.APPROVED);
        review.setReviewedAt(now());
        return crawlerReviewsRepository.save(review);
    }

    private CrawlerReviewBean reject(CrawlerReviewBean review) {
        if (review.getReviewStatus() != ReviewStatus.PENDING) {
            throw badRequest("只有待審核資料可以拒絕");
        }

        review.setReviewStatus(ReviewStatus.REJECTED);
        review.setReviewedAt(now());
        return crawlerReviewsRepository.save(review);
    }

    private CrawlerReviewBean reopen(CrawlerReviewBean review) {
        if (review.getReviewStatus() == ReviewStatus.PENDING) {
            throw badRequest("此資料已是待審核");
        }

        boolean hasPending = crawlerReviewsRepository
                .findByCrawlerTypeAndEntityIdAndFieldName(
                        review.getCrawlerType(),
                        review.getEntityId(),
                        review.getFieldName())
                .stream()
                .anyMatch(existing -> existing.getReviewStatus() == ReviewStatus.PENDING);

        if (hasPending) {
            throw badRequest("此欄位已有待審核資料");
        }

        String currentValue = currentOfficialValue(review);

        if (Objects.equals(currentValue, review.getCrawlerValue())) {
            throw badRequest("正式資料已與爬蟲值相同，無需重新審核");
        }

        CrawlerReviewBean pending = new CrawlerReviewBean();
        pending.setCrawlerType(review.getCrawlerType());
        pending.setEntityId(review.getEntityId());
        pending.setFieldName(review.getFieldName());
        pending.setCrawlerValue(review.getCrawlerValue());
        pending.setPreviousValue(currentValue);
        pending.setReviewStatus(ReviewStatus.PENDING);
        return crawlerReviewsRepository.save(pending);
    }

    private void applyOfficialValue(CrawlerReviewBean review) {
        try {
            switch (review.getCrawlerType()) {
                case CINEMA -> cinemaService.updateCinema(
                        review.getEntityId(),
                        cinemaRequest(review.getFieldName(), review.getCrawlerValue()));
                case MOVIE -> movieService.updateMovie(
                        review.getEntityId(),
                        movieRequest(review.getFieldName(), review.getCrawlerValue()));
            }
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw badRequest("爬蟲值無法寫入正式資料：" + review.getFieldName());
        }
    }

    private boolean isMovieSourceConflict(CrawlerReviewBean review) {
        return review.getCrawlerType() == CinemaResourceType.MOVIE
                && SOURCE_ID.equals(review.getFieldName());
    }

    private String currentOfficialValue(CrawlerReviewBean review) {
        return switch (review.getCrawlerType()) {
            case CINEMA -> cinemaField(
                    cinemaService.findById(review.getEntityId()),
                    review.getFieldName());
            case MOVIE -> movieField(
                    movieService.findById(review.getEntityId()),
                    review.getFieldName());
        };
    }

    private CinemaRequest cinemaRequest(String fieldName, String crawlerValue) {
        return switch (fieldName) {
            case CINEMA_NAME -> new CinemaRequest(
                    null, requireText(crawlerValue), null, null, null, null, null, null, null);
            case CINEMA_ADDRESS -> new CinemaRequest(
                    null, null, null, crawlerValue, null, null, null, null, null);
            case COUNTY -> new CinemaRequest(
                    null, null, County.valueOf(crawlerValue), null, null, null, null, null, null);
            case LATITUDE -> new CinemaRequest(
                    null, null, null, null, new BigDecimal(crawlerValue), null, null, null, null);
            case LONGITUDE -> new CinemaRequest(
                    null, null, null, null, null, new BigDecimal(crawlerValue), null, null, null);
            case OFFICIAL_WEBSITE_URL -> new CinemaRequest(
                    null, null, null, null, null, null, crawlerValue, null, null);
            case IS_ACTIVE -> new CinemaRequest(
                    null, null, null, null, null, null, null, parseBoolean(crawlerValue), null);
            case IS_VISIBLE -> new CinemaRequest(
                    null, null, null, null, null, null, null, null, parseBoolean(crawlerValue));
            default -> throw badRequest("不支援的影城欄位：" + fieldName);
        };
    }

    private MovieRequest movieRequest(String fieldName, String crawlerValue) {
        return switch (fieldName) {
            case MOVIE_NAME -> new MovieRequest(null, requireText(crawlerValue), null, null, null);
            case IMAGE_URL -> new MovieRequest(null, null, crawlerValue, null, null);
            case RELEASE_DATE -> new MovieRequest(
                    null, null, null, null, LocalDateTime.parse(crawlerValue, DATE_TIME));
            case IS_VISIBLE -> new MovieRequest(null, null, null, parseBoolean(crawlerValue), null);
            default -> throw badRequest("不支援的電影欄位：" + fieldName);
        };
    }

    private String cinemaField(CinemaResponse cinema, String fieldName) {
        return switch (fieldName) {
            case CINEMA_NAME -> formatValue(cinema.cinemaName());
            case CINEMA_ADDRESS -> formatValue(cinema.cinemaAddress());
            case COUNTY -> formatValue(cinema.county());
            case LATITUDE -> formatValue(cinema.latitude());
            case LONGITUDE -> formatValue(cinema.longitude());
            case OFFICIAL_WEBSITE_URL -> formatValue(cinema.officialWebsiteUrl());
            case IS_ACTIVE -> formatValue(cinema.isActive());
            case IS_VISIBLE -> formatValue(cinema.isVisible());
            default -> throw badRequest("不支援的影城欄位：" + fieldName);
        };
    }

    private String movieField(MovieResponse movie, String fieldName) {
        return switch (fieldName) {
            case SOURCE_ID -> formatValue(movie.sourceId());
            case MOVIE_NAME -> formatValue(movie.movieName());
            case IMAGE_URL -> formatValue(movie.imageUrl());
            case RELEASE_DATE -> formatValue(movie.releaseDate());
            case IS_VISIBLE -> formatValue(movie.isVisible());
            default -> throw badRequest("不支援的電影欄位：" + fieldName);
        };
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("空值");
        }

        return value;
    }

    private Boolean parseBoolean(String value) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException("布林值");
        }

        return Boolean.valueOf(value);
    }

    private boolean sameDifference(CrawlerReviewBean review, String crawlerValue, String previousValue) {
        return Objects.equals(review.getCrawlerValue(), crawlerValue)
                && Objects.equals(review.getPreviousValue(), previousValue);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }
}
