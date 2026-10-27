package com.collection.benefitsjourney.service.CinemaMovie;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.CinemaMovieReviewBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewResponse;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieRequest;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.repository.CinemaMovieReviewsRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaMovieReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;

@Service
public class CinemaMovieReviewsService {
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
    public static final String SOURCE_URL = "source_url";
    public static final String MOVIE_NAME = "movie_name";
    public static final String IMAGE_URL = "image_url";
    public static final String RELEASE_DATE = "release_date";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CinemaMovieReviewsRepository cinemaMovieReviewsRepository;
    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;
    private final CinemaService cinemaService;
    private final MovieService movieService;

    public CinemaMovieReviewsService(
            CinemaMovieReviewsRepository cinemaMovieReviewsRepository,
            CinemaRepository cinemaRepository,
            MovieRepository movieRepository,
            CinemaService cinemaService,
            MovieService movieService) {
        this.cinemaMovieReviewsRepository = cinemaMovieReviewsRepository;
        this.cinemaRepository = cinemaRepository;
        this.movieRepository = movieRepository;
        this.cinemaService = cinemaService;
        this.movieService = movieService;
    }

    /** 依條件查詢爬蟲審核 */
    public PageResponse<CinemaMovieReviewResponse> findAll(
            CinemaResourceType crawlerType,
            ReviewStatus reviewStatus,
            int page,
            int size,
            CinemaMovieReviewSort.SortBy orderBy,
            SortOrder sort) {

        Page<CinemaMovieReviewBean> result = findPage(
                crawlerType, reviewStatus, page, size, orderBy, sort);
        EntityNames names = entityNames(result.getContent());

        return PageResponse.from(result.map(review -> toResponse(review, names)));
    }

    private Page<CinemaMovieReviewBean> findPage(
            CinemaResourceType crawlerType,
            ReviewStatus reviewStatus,
            int page,
            int size,
            CinemaMovieReviewSort.SortBy orderBy,
            SortOrder sort) {

        Sort.Direction direction = sort == SortOrder.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort order = Sort.by(direction, orderBy.getProperty());

        // 相同排序值以審核 ID 排序
        if (orderBy != CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID) {
            order = order.and(Sort.by(
                    Sort.Direction.ASC,
                    TypedPropertyPath.path((CinemaMovieReviewBean review) -> review.getCinemaMovieReviewId())));
        }

        PageRequest pageable = PageRequest.of(page - 1, size, order);

        return cinemaMovieReviewsRepository.findAllByFilters(
                crawlerType,
                reviewStatus,
                pageable);
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
    public CinemaMovieReviewResponse createCinemaMovieReview(CinemaMovieReviewRequest request) {
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
                .map(this::toResponse)
                .orElseThrow(() -> badRequest("已有相同差異的審核紀錄，不重複新增"));
    }

    /** 審核通過：待審核寫入正式資料；已審核則拒絕 */
    @Transactional
    public CinemaMovieReviewResponse approveCinemaMovieReview(Integer id) {
        CinemaMovieReviewBean review = cinemaMovieReviewsRepository.findById(id)
                .orElseThrow(() -> notFound("找不到爬蟲審核ID：" + id));

        if (review.getReviewStatus() != ReviewStatus.PENDING) {
            throw badRequest("你已審核過");
        }

        // 來源衝突只確認重複，不修改電影
        if (review.getCrawlerType() == CinemaResourceType.MOVIE
                && SOURCE_ID.equals(review.getFieldName())) {
            requireText(review.getCrawlerValue());
            movieService.findById(review.getEntityId());
        } else {
            try {
                switch (review.getCrawlerType()) {
                    case CINEMA -> cinemaService.updateCinema(
                            review.getEntityId(),
                            cinemaRequest(review.getFieldName(), review.getCrawlerValue()));
                    case MOVIE -> movieService.updateMovie(
                            review.getEntityId(),
                            movieRequest(review.getFieldName(), review.getCrawlerValue()));
                    case CRAWLER_LOG -> throw badRequest("爬蟲紀錄無法寫入影城或電影資料");
                    case CINEMA_MOVIE_REVIEW -> throw badRequest("爬蟲審核無法寫入影城或電影資料");
                }
            } catch (IllegalArgumentException | DateTimeParseException exception) {
                throw badRequest("爬蟲值無法寫入正式資料：" + review.getFieldName());
            }
        }

        review.setReviewStatus(ReviewStatus.APPROVED);
        review.setReviewedAt(LocalDateTime.now().withNano(0));
        return toResponse(cinemaMovieReviewsRepository.save(review));
    }

    /** 審核拒絕：只改成已拒絕，不寫入正式資料 */
    @Transactional
    public CinemaMovieReviewResponse rejectCinemaMovieReview(Integer id) {
        CinemaMovieReviewBean review = cinemaMovieReviewsRepository.findById(id)
                .orElseThrow(() -> notFound("找不到爬蟲審核ID：" + id));

        if (review.getReviewStatus() != ReviewStatus.PENDING) {
            throw badRequest("只有待審核資料可以拒絕");
        }

        review.setReviewStatus(ReviewStatus.REJECTED);
        review.setReviewedAt(LocalDateTime.now().withNano(0));
        return toResponse(cinemaMovieReviewsRepository.save(review));
    }

    private CinemaMovieReviewResponse toResponse(CinemaMovieReviewBean review) {
        return toResponse(review, entityNames(List.of(review)));
    }

    private CinemaMovieReviewResponse toResponse(CinemaMovieReviewBean review, EntityNames names) {
        String entityName = null;
        if (review.getEntityId() != null && review.getCrawlerType() != null) {
            entityName = switch (review.getCrawlerType()) {
                case CINEMA -> names.cinemas().get(review.getEntityId());
                case MOVIE -> names.movies().get(review.getEntityId());
                case CRAWLER_LOG, CINEMA_MOVIE_REVIEW -> null;
            };
        }
        return CinemaMovieReviewResponse.from(review, entityName);
    }

    /** 依爬蟲類型批次取出電影或影城名稱；兩種編號分開記，避免同號互相覆蓋 */
    private EntityNames entityNames(List<CinemaMovieReviewBean> reviews) {
        return new EntityNames(
                namesById(
                        idsOf(reviews, CinemaResourceType.CINEMA),
                        cinemaRepository::findAllById,
                        (CinemaBean cinema) -> cinema.getCinemaId(),
                        (CinemaBean cinema) -> cinema.getCinemaName()),
                namesById(
                        idsOf(reviews, CinemaResourceType.MOVIE),
                        movieRepository::findAllById,
                        (MovieBean movie) -> movie.getMovieId(),
                        (MovieBean movie) -> movie.getMovieName()));
    }

    private record EntityNames(Map<Integer, String> cinemas, Map<Integer, String> movies) {
    }

    private static Set<Integer> idsOf(List<CinemaMovieReviewBean> reviews, CinemaResourceType type) {
        Set<Integer> ids = new HashSet<>();
        for (CinemaMovieReviewBean review : reviews) {
            if (review.getCrawlerType() == type && review.getEntityId() != null) {
                ids.add(review.getEntityId());
            }
        }
        return ids;
    }

    private static <T> Map<Integer, String> namesById(
            Set<Integer> ids,
            Function<Set<Integer>, List<T>> finder,
            Function<T, Integer> idOf,
            Function<T, String> nameOf) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<T> entities = finder.apply(ids);
        if (entities == null || entities.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> names = new HashMap<>();
        for (T entity : entities) {
            if (entity == null || idOf.apply(entity) == null) {
                continue;
            }
            names.put(idOf.apply(entity), nameOf.apply(entity));
        }
        return names;
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

    private Optional<CinemaMovieReviewBean> saveCrawlerChange(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName,
            String crawlerValue,
            String previousValue) {
        if (Objects.equals(crawlerValue, previousValue)) {
            return Optional.empty();
        }

        List<CinemaMovieReviewBean> reviews = cinemaMovieReviewsRepository
                .findByCrawlerTypeAndEntityIdAndFieldName(crawlerType, entityId, fieldName);

        CinemaMovieReviewBean pending = reviews.stream()
                .filter(review -> review.getReviewStatus() == ReviewStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (pending != null) {
            if (sameDifference(pending, crawlerValue, previousValue)) {
                return Optional.empty();
            }

            pending.setCrawlerValue(crawlerValue);
            pending.setPreviousValue(previousValue);
            return Optional.of(cinemaMovieReviewsRepository.save(pending));
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

        CinemaMovieReviewBean review = new CinemaMovieReviewBean();
        review.setCrawlerType(crawlerType);
        review.setEntityId(entityId);
        review.setFieldName(fieldName);
        review.setCrawlerValue(crawlerValue);
        review.setPreviousValue(previousValue);
        review.setReviewStatus(ReviewStatus.PENDING);

        return Optional.of(cinemaMovieReviewsRepository.save(review));
    }

    private CinemaRequest cinemaRequest(String fieldName, String crawlerValue) {
        return switch (fieldName) {
            case CINEMA_NAME -> new CinemaRequest(
                    null, null, requireText(crawlerValue), null, null, null, null, null, null, null);
            case CINEMA_ADDRESS -> new CinemaRequest(
                    null, null, null, null, crawlerValue, null, null, null, null, null);
            case COUNTY -> new CinemaRequest(
                    null, null, null, County.valueOf(crawlerValue), null, null, null, null, null, null);
            case LATITUDE -> new CinemaRequest(
                    null, null, null, null, null, new BigDecimal(crawlerValue), null, null, null, null);
            case LONGITUDE -> new CinemaRequest(
                    null, null, null, null, null, null, new BigDecimal(crawlerValue), null, null, null);
            case OFFICIAL_WEBSITE_URL -> new CinemaRequest(
                    null, null, null, null, null, null, null, crawlerValue, null, null);
            case SOURCE_URL -> new CinemaRequest(
                    null, requireText(crawlerValue), null, null, null, null, null, null, null, null);
            case IS_ACTIVE -> new CinemaRequest(
                    null, null, null, null, null, null, null, null, parseBoolean(crawlerValue), null);
            case IS_VISIBLE -> new CinemaRequest(
                    null, null, null, null, null, null, null, null, null, parseBoolean(crawlerValue));
            default -> throw badRequest("不支援的影城欄位：" + fieldName);
        };
    }

    private MovieRequest movieRequest(String fieldName, String crawlerValue) {
        return switch (fieldName) {
            case MOVIE_NAME -> new MovieRequest(null, null, requireText(crawlerValue), null, null, null);
            case IMAGE_URL -> new MovieRequest(null, null, null, crawlerValue, null, null);
            case RELEASE_DATE -> new MovieRequest(
                    null, null, null, null, null, LocalDateTime.parse(crawlerValue, DATE_TIME));
            case SOURCE_URL -> new MovieRequest(null, requireText(crawlerValue), null, null, null, null);
            case IS_VISIBLE -> new MovieRequest(null, null, null, null, parseBoolean(crawlerValue), null);
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

    private boolean sameDifference(CinemaMovieReviewBean review, String crawlerValue, String previousValue) {
        return Objects.equals(review.getCrawlerValue(), crawlerValue)
                && Objects.equals(review.getPreviousValue(), previousValue);
    }
}
