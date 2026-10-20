package com.collection.benefitsjourney.service.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.CinemaMovieReviewBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieReviewResponse;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaMovieReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CinemaMovieReviewsRepository;

@ExtendWith(MockitoExtension.class)
class CinemaMovieReviewsServiceTest {

    @Mock
    private CinemaMovieReviewsRepository cinemaMovieReviewsRepository;

    @Mock
    private CinemaService cinemaService;

    @Mock
    private MovieService movieService;

    @InjectMocks
    private CinemaMovieReviewsService cinemaMovieReviewsService;

    private static final LocalDateTime REVIEWED_AT = LocalDateTime.of(2026, 10, 9, 11, 0);

    private CinemaMovieReviewBean review(Integer id, ReviewStatus status) {
        CinemaMovieReviewBean review = new CinemaMovieReviewBean();
        review.setCinemaMovieReviewId(id);
        review.setCrawlerType(CinemaResourceType.MOVIE);
        review.setEntityId(12);
        review.setFieldName("movieName");
        review.setCrawlerValue("新片名");
        review.setPreviousValue("舊片名");
        review.setReviewStatus(status);
        review.setReviewedAt(REVIEWED_AT);
        review.setCreatedAt(LocalDateTime.of(2026, 10, 1, 8, 0));
        review.setUpdatedAt(LocalDateTime.of(2026, 10, 2, 9, 0));
        return review;
    }

    private CinemaMovieReviewRequest request(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName,
            String crawlerValue,
            String previousValue,
            ReviewStatus reviewStatus,
            LocalDateTime reviewedAt) {
        return new CinemaMovieReviewRequest(
                crawlerType,
                entityId,
                fieldName,
                crawlerValue,
                previousValue,
                reviewStatus,
                reviewedAt);
    }

    @Nested
    @DisplayName("findAll - 查詢、分頁與排序")
    class FindAllTests {

        @Test
        @DisplayName("查詢參數應傳到 Repository，頁碼應從 1-based 轉 0-based")
        void shouldPassFiltersAndPaging() {
            CinemaMovieReviewBean bean = review(7, ReviewStatus.PENDING);
            when(cinemaMovieReviewsRepository.findAllByFilters(
                    eq(CinemaResourceType.MOVIE),
                    eq(ReviewStatus.PENDING),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(bean),
                            PageRequest.of(2, 20),
                            41));

            PageResponse<CinemaMovieReviewResponse> result = cinemaMovieReviewsService.findAll(
                    CinemaResourceType.MOVIE,
                    ReviewStatus.PENDING,
                    3,
                    20,
                    CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID,
                    SortOrder.ASC);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(cinemaMovieReviewsRepository).findAllByFilters(
                    eq(CinemaResourceType.MOVIE),
                    eq(ReviewStatus.PENDING),
                    pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertEquals(2, pageable.getPageNumber());
            assertEquals(20, pageable.getPageSize());
            assertEquals(1, pageable.getSort().stream().count());
            Sort.Order order = pageable.getSort().getOrderFor("cinemaMovieReviewId");
            assertNotNull(order);
            assertEquals(Sort.Direction.ASC, order.getDirection());

            assertEquals(3, result.page());
            assertEquals(20, result.size());
            assertEquals(41, result.totalElements());
            assertEquals(3, result.totalPages());
            assertEquals(1, result.content().size());
            CinemaMovieReviewResponse response = result.content().get(0);
            assertEquals(7, response.cinemaMovieReviewId());
            assertEquals(CinemaResourceType.MOVIE, response.crawlerType());
            assertEquals(12, response.entityId());
            assertEquals("movieName", response.fieldName());
            assertEquals("新片名", response.crawlerValue());
            assertEquals("舊片名", response.previousValue());
            assertEquals(ReviewStatus.PENDING, response.reviewStatus());
            assertEquals(REVIEWED_AT, response.reviewedAt());
            assertEquals(bean.getCreatedAt(), response.createdAt());
            assertEquals(bean.getUpdatedAt(), response.updatedAt());
        }

        @Test
        @DisplayName("未指定篩選時應把 null 傳給 Repository")
        void shouldAllowNullFilters() {
            when(cinemaMovieReviewsRepository.findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            PageResponse<CinemaMovieReviewResponse> result = cinemaMovieReviewsService.findAll(
                    null,
                    null,
                    1,
                    10,
                    CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID,
                    SortOrder.DESC);

            assertNotNull(result);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(cinemaMovieReviewsRepository).findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertEquals(0, pageable.getPageNumber());
            assertEquals(10, pageable.getPageSize());
            assertEquals(1, pageable.getSort().stream().count());
            assertEquals(Sort.Direction.DESC,
                    pageable.getSort().getOrderFor("cinemaMovieReviewId").getDirection());
        }

        @Test
        @DisplayName("非主鍵排序時，應再依 cinemaMovieReviewId 由小到大")
        void shouldAppendIdSort() {
            when(cinemaMovieReviewsRepository.findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            cinemaMovieReviewsService.findAll(
                    null,
                    null,
                    1,
                    10,
                    CinemaMovieReviewSort.SortBy.REVIEW_STATUS,
                    SortOrder.DESC);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(cinemaMovieReviewsRepository).findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    pageableCaptor.capture());

            List<Sort.Order> orders = pageableCaptor.getValue().getSort().toList();
            assertEquals(2, orders.size());
            assertEquals("reviewStatus", orders.get(0).getProperty());
            assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
            assertEquals("cinemaMovieReviewId", orders.get(1).getProperty());
            assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
        }
    }

    @Nested
    @DisplayName("createCinemaMovieReview - 新增爬蟲審核")
    class CreateCinemaMovieReviewTests {

        @Test
        @DisplayName("Request 為 null 應回傳 400，且不呼叫 Repository")
        void nullRequest() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(null));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("請求內容不可為空", exception.getReason());
            verifyNoInteractions(cinemaMovieReviewsRepository);
        }

        @Test
        @DisplayName("爬蟲類型未填應回傳 400")
        void nullCrawlerType() {
            CinemaMovieReviewRequest request = request(
                    null, 12, "movieName", null, null, null, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("爬蟲類型不可為空", exception.getReason());
            verifyNoInteractions(cinemaMovieReviewsRepository);
        }

        @Test
        @DisplayName("對應資料 ID 未填應回傳 400")
        void nullEntityId() {
            CinemaMovieReviewRequest request = request(
                    CinemaResourceType.MOVIE, null, "movieName", null, null, null, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("對應資料ID不可為空", exception.getReason());
            verifyNoInteractions(cinemaMovieReviewsRepository);
        }

        @Test
        @DisplayName("欄位名稱未填或只有空白應回傳 400")
        void blankFieldName() {
            CinemaMovieReviewRequest nullName = request(
                    CinemaResourceType.MOVIE, 12, null, null, null, null, null);
            CinemaMovieReviewRequest blankName = request(
                    CinemaResourceType.MOVIE, 12, "   ", null, null, null, null);

            ResponseStatusException nullNameException = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(nullName));
            ResponseStatusException blankNameException = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(blankName));

            assertEquals(HttpStatus.BAD_REQUEST, nullNameException.getStatusCode());
            assertEquals("欄位名稱不可為空", nullNameException.getReason());
            assertEquals("欄位名稱不可為空", blankNameException.getReason());
            verifyNoInteractions(cinemaMovieReviewsRepository);
        }

        @Test
        @DisplayName("相同差異已有待審或已拒絕時應回傳 400，不能再新增")
        void duplicateSameDifference() {
            CinemaMovieReviewBean pending = review(2, ReviewStatus.PENDING);
            CinemaMovieReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名", null, null);
            when(cinemaMovieReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(review(1, ReviewStatus.APPROVED), pending));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("已有相同差異的審核紀錄，不重複新增", exception.getReason());
            verify(cinemaMovieReviewsRepository, never()).save(any(CinemaMovieReviewBean.class));
        }

        @Test
        @DisplayName("爬蟲值與原值相同時應回傳 400")
        void sameValue() {
            CinemaMovieReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "同一片名", "同一片名", null, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("爬蟲值與原值相同，無需新增", exception.getReason());
            verifyNoInteractions(cinemaMovieReviewsRepository);
        }

        @Test
        @DisplayName("直接新增已拒絕或已接受應回傳 400")
        void rejectNonPendingCreate() {
            CinemaMovieReviewRequest request = request(
                    CinemaResourceType.MOVIE,
                    8,
                    "movieName",
                    "新片名",
                    "舊片名",
                    ReviewStatus.REJECTED,
                    REVIEWED_AT);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.createCinemaMovieReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("新增僅能建立待審核資料", exception.getReason());
            verifyNoInteractions(cinemaMovieReviewsRepository);
        }

        @Test
        @DisplayName("沒有相同差異時應新增 PENDING，已接受的歷史不擋新增")
        void shouldCreatePending() {
            CinemaMovieReviewRequest request = request(
                    CinemaResourceType.CINEMA,
                    100,
                    "cinema_address",
                    "新地址",
                    "舊地址",
                    null,
                    null);
            when(cinemaMovieReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.CINEMA, 100, "cinema_address"))
                    .thenReturn(List.of(review(1, ReviewStatus.APPROVED)));
            when(cinemaMovieReviewsRepository.save(any(CinemaMovieReviewBean.class)))
                    .thenAnswer(invocation -> {
                        CinemaMovieReviewBean saved = invocation.getArgument(0);
                        saved.setCinemaMovieReviewId(15);
                        return saved;
                    });

            CinemaMovieReviewResponse result = cinemaMovieReviewsService.createCinemaMovieReview(request);

            ArgumentCaptor<CinemaMovieReviewBean> captor = ArgumentCaptor.forClass(CinemaMovieReviewBean.class);
            verify(cinemaMovieReviewsRepository).save(captor.capture());
            CinemaMovieReviewBean saved = captor.getValue();
            assertEquals(CinemaResourceType.CINEMA, saved.getCrawlerType());
            assertEquals(100, saved.getEntityId());
            assertEquals("cinema_address", saved.getFieldName());
            assertEquals("新地址", saved.getCrawlerValue());
            assertEquals("舊地址", saved.getPreviousValue());
            assertEquals(ReviewStatus.PENDING, saved.getReviewStatus());
            assertEquals(15, result.cinemaMovieReviewId());
        }

        @Test
        @DisplayName("同欄位已有 PENDING 但爬蟲值改變時，應更新該筆而不是再新增")
        void shouldUpdateExistingPending() {
            CinemaMovieReviewBean pending = review(2, ReviewStatus.PENDING);
            CinemaMovieReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "更新後片名", "舊片名", null, null);
            when(cinemaMovieReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(pending));
            when(cinemaMovieReviewsRepository.save(any(CinemaMovieReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaMovieReviewResponse result = cinemaMovieReviewsService.createCinemaMovieReview(request);

            assertEquals(2, result.cinemaMovieReviewId());
            assertEquals("更新後片名", pending.getCrawlerValue());
            assertEquals("舊片名", pending.getPreviousValue());
            assertEquals(ReviewStatus.PENDING, pending.getReviewStatus());
            assertEquals("更新後片名", result.crawlerValue());
            verify(cinemaMovieReviewsRepository).save(pending);
        }
    }

    @Nested
    @DisplayName("submitCrawlerChange - 爬蟲匯入去重")
    class SubmitCrawlerChangeTests {

        @Test
        @DisplayName("相同差異已是 REJECTED 時應略過，不再每天新增")
        void skipRejectedDifference() {
            CinemaMovieReviewBean rejected = review(3, ReviewStatus.REJECTED);
            when(cinemaMovieReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(rejected));

            boolean saved = cinemaMovieReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名");

            assertEquals(false, saved);
            verify(cinemaMovieReviewsRepository, never()).save(any(CinemaMovieReviewBean.class));
        }

        @Test
        @DisplayName("已接受的電影來源衝突，相同差異不再送審")
        void skipApprovedSourceConflict() {
            CinemaMovieReviewBean approved = review(4, ReviewStatus.APPROVED);
            approved.setFieldName(CinemaMovieReviewsService.SOURCE_ID);
            approved.setCrawlerValue("fNEW");
            approved.setPreviousValue("fOLD");
            when(cinemaMovieReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, CinemaMovieReviewsService.SOURCE_ID))
                    .thenReturn(List.of(approved));

            boolean saved = cinemaMovieReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, CinemaMovieReviewsService.SOURCE_ID, "fNEW", "fOLD");

            assertEquals(false, saved);
            verify(cinemaMovieReviewsRepository, never()).save(any(CinemaMovieReviewBean.class));
        }

        @Test
        @DisplayName("已接受的一般欄位不擋相同差異再次送審")
        void approvedMovieNameDoesNotBlockResubmit() {
            CinemaMovieReviewBean approved = review(4, ReviewStatus.APPROVED);
            when(cinemaMovieReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(approved));
            when(cinemaMovieReviewsRepository.save(any(CinemaMovieReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            boolean saved = cinemaMovieReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名");

            assertEquals(true, saved);
            ArgumentCaptor<CinemaMovieReviewBean> captor = ArgumentCaptor.forClass(CinemaMovieReviewBean.class);
            verify(cinemaMovieReviewsRepository).save(captor.capture());
            assertEquals(ReviewStatus.PENDING, captor.getValue().getReviewStatus());
            assertEquals("新片名", captor.getValue().getCrawlerValue());
        }

        @Test
        @DisplayName("已接受的來源衝突若新來源不同，仍要再送審")
        void approvedSourceConflictWithNewValue() {
            CinemaMovieReviewBean approved = review(4, ReviewStatus.APPROVED);
            approved.setFieldName(CinemaMovieReviewsService.SOURCE_ID);
            approved.setCrawlerValue("fNEW");
            approved.setPreviousValue("fOLD");
            when(cinemaMovieReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, CinemaMovieReviewsService.SOURCE_ID))
                    .thenReturn(List.of(approved));
            when(cinemaMovieReviewsRepository.save(any(CinemaMovieReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            boolean saved = cinemaMovieReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, CinemaMovieReviewsService.SOURCE_ID, "fOTHER", "fOLD");

            assertEquals(true, saved);
            ArgumentCaptor<CinemaMovieReviewBean> captor = ArgumentCaptor.forClass(CinemaMovieReviewBean.class);
            verify(cinemaMovieReviewsRepository).save(captor.capture());
            assertEquals("fOTHER", captor.getValue().getCrawlerValue());
            assertEquals("fOLD", captor.getValue().getPreviousValue());
            assertEquals(ReviewStatus.PENDING, captor.getValue().getReviewStatus());
        }
    }

    @Nested
    @DisplayName("approveCinemaMovieReview - 審核通過")
    class ApproveCinemaMovieReviewTests {

        @Test
        @DisplayName("待審核通過時應寫入正式電影並改成 APPROVED")
        void approvePendingMovie() {
            CinemaMovieReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setReviewedAt(null);
            existing.setFieldName(CinemaMovieReviewsService.MOVIE_NAME);
            when(cinemaMovieReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(movieService.updateMovie(eq(12), any(MovieRequest.class)))
                    .thenReturn(new MovieResponse(
                            12, null, null, "新片名", null, null, null, null, null));
            when(cinemaMovieReviewsRepository.save(any(CinemaMovieReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaMovieReviewResponse result = cinemaMovieReviewsService.approveCinemaMovieReview(7);

            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).updateMovie(eq(12), captor.capture());
            assertEquals("新片名", captor.getValue().movieName());
            assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
            assertNotNull(existing.getReviewedAt());
            assertEquals(ReviewStatus.APPROVED, result.reviewStatus());
        }

        @Test
        @DisplayName("不是待審核時應回傳你已審核過")
        void alreadyReviewed() {
            CinemaMovieReviewBean existing = review(7, ReviewStatus.APPROVED);
            when(cinemaMovieReviewsRepository.findById(7)).thenReturn(Optional.of(existing));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.approveCinemaMovieReview(7));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("你已審核過", exception.getReason());
            assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
            verify(cinemaMovieReviewsRepository, never()).save(any(CinemaMovieReviewBean.class));
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("找不到審核應回傳 404")
        void notFound() {
            when(cinemaMovieReviewsRepository.findById(999)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.approveCinemaMovieReview(999));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到爬蟲審核ID：999", exception.getReason());
        }

        @Test
        @DisplayName("接受電影來源衝突時只確認電影存在，不覆蓋正式來源")
        void approveSourceConflictWithoutUpdatingMovie() {
            CinemaMovieReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setReviewedAt(null);
            existing.setFieldName(CinemaMovieReviewsService.SOURCE_ID);
            existing.setCrawlerValue("fNEW");
            existing.setPreviousValue("fOLD");
            when(cinemaMovieReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(movieService.findById(12)).thenReturn(new MovieResponse(
                    12, "fOLD", null, "既有片名", null, true, null, null, null));
            when(cinemaMovieReviewsRepository.save(any(CinemaMovieReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaMovieReviewResponse result = cinemaMovieReviewsService.approveCinemaMovieReview(7);

            assertEquals(ReviewStatus.APPROVED, result.reviewStatus());
            assertNotNull(existing.getReviewedAt());
            verify(movieService).findById(12);
            verify(movieService, never()).updateMovie(any(), any());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("來源衝突的新來源是空值時不能接受")
        void approveBlankSourceConflict() {
            CinemaMovieReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setFieldName(CinemaMovieReviewsService.SOURCE_ID);
            existing.setCrawlerValue("  ");
            when(cinemaMovieReviewsRepository.findById(7)).thenReturn(Optional.of(existing));

            assertThrows(IllegalArgumentException.class, () -> cinemaMovieReviewsService.approveCinemaMovieReview(7));

            assertEquals(ReviewStatus.PENDING, existing.getReviewStatus());
            verify(movieService, never()).findById(any());
            verify(cinemaMovieReviewsRepository, never()).save(any(CinemaMovieReviewBean.class));
        }
    }

    @Nested
    @DisplayName("rejectCinemaMovieReview - 審核拒絕")
    class RejectCinemaMovieReviewTests {

        @Test
        @DisplayName("待審核拒絕時只改成 REJECTED，不寫入正式資料")
        void rejectPending() {
            CinemaMovieReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setReviewedAt(null);
            when(cinemaMovieReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(cinemaMovieReviewsRepository.save(any(CinemaMovieReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaMovieReviewResponse result = cinemaMovieReviewsService.rejectCinemaMovieReview(7);

            assertEquals(ReviewStatus.REJECTED, existing.getReviewStatus());
            assertEquals("新片名", existing.getCrawlerValue());
            assertEquals("舊片名", existing.getPreviousValue());
            assertNotNull(existing.getReviewedAt());
            assertEquals(ReviewStatus.REJECTED, result.reviewStatus());
            verify(cinemaMovieReviewsRepository).save(existing);
            verifyNoInteractions(movieService);
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("不是待審核時不能拒絕")
        void alreadyReviewed() {
            CinemaMovieReviewBean existing = review(7, ReviewStatus.APPROVED);
            when(cinemaMovieReviewsRepository.findById(7)).thenReturn(Optional.of(existing));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.rejectCinemaMovieReview(7));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("只有待審核資料可以拒絕", exception.getReason());
            assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
            verify(cinemaMovieReviewsRepository, never()).save(any(CinemaMovieReviewBean.class));
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("找不到審核應回傳 404")
        void notFound() {
            when(cinemaMovieReviewsRepository.findById(999)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMovieReviewsService.rejectCinemaMovieReview(999));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到爬蟲審核ID：999", exception.getReason());
        }
    }
}
