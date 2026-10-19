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

import com.collection.benefitsjourney.domain.CinemaMovieCrawlerReviewBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerReviewRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMovieCrawlerReviewResponse;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.MovieResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaMovieCrawlerReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CinemaMovieCrawlerReviewsRepository;

@ExtendWith(MockitoExtension.class)
class CinemaMovieCrawlerReviewsServiceTest {

    @Mock
    private CinemaMovieCrawlerReviewsRepository crawlerReviewsRepository;

    @Mock
    private CinemaService cinemaService;

    @Mock
    private MovieService movieService;

    @InjectMocks
    private CinemaMovieCrawlerReviewsService crawlerReviewsService;

    private static final LocalDateTime REVIEWED_AT = LocalDateTime.of(2026, 10, 9, 11, 0);

    private CinemaMovieCrawlerReviewBean review(Integer id, ReviewStatus status) {
        CinemaMovieCrawlerReviewBean review = new CinemaMovieCrawlerReviewBean();
        review.setCrawlerReviewId(id);
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

    private CinemaMovieCrawlerReviewRequest request(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName,
            String crawlerValue,
            String previousValue,
            ReviewStatus reviewStatus,
            LocalDateTime reviewedAt) {
        return new CinemaMovieCrawlerReviewRequest(
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
            CinemaMovieCrawlerReviewBean bean = review(7, ReviewStatus.PENDING);
            when(crawlerReviewsRepository.findAllByFilters(
                    eq(CinemaResourceType.MOVIE),
                    eq(ReviewStatus.PENDING),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(bean),
                            PageRequest.of(2, 20),
                            41));

            PageResponse<CinemaMovieCrawlerReviewResponse> result = crawlerReviewsService.findAll(
                    CinemaResourceType.MOVIE,
                    ReviewStatus.PENDING,
                    3,
                    20,
                    CinemaMovieCrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID,
                    SortOrder.ASC);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(crawlerReviewsRepository).findAllByFilters(
                    eq(CinemaResourceType.MOVIE),
                    eq(ReviewStatus.PENDING),
                    pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertEquals(2, pageable.getPageNumber());
            assertEquals(20, pageable.getPageSize());
            assertEquals(1, pageable.getSort().stream().count());
            Sort.Order order = pageable.getSort().getOrderFor("crawlerReviewId");
            assertNotNull(order);
            assertEquals(Sort.Direction.ASC, order.getDirection());

            assertEquals(3, result.page());
            assertEquals(20, result.size());
            assertEquals(41, result.totalElements());
            assertEquals(3, result.totalPages());
            assertEquals(1, result.content().size());
            CinemaMovieCrawlerReviewResponse response = result.content().get(0);
            assertEquals(7, response.crawlerReviewId());
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
            when(crawlerReviewsRepository.findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            PageResponse<CinemaMovieCrawlerReviewResponse> result = crawlerReviewsService.findAll(
                    null,
                    null,
                    1,
                    10,
                    CinemaMovieCrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID,
                    SortOrder.DESC);

            assertNotNull(result);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(crawlerReviewsRepository).findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertEquals(0, pageable.getPageNumber());
            assertEquals(10, pageable.getPageSize());
            assertEquals(1, pageable.getSort().stream().count());
            assertEquals(Sort.Direction.DESC,
                    pageable.getSort().getOrderFor("crawlerReviewId").getDirection());
        }

        @Test
        @DisplayName("非主鍵排序時，應再依 crawlerReviewId 由小到大")
        void shouldAppendIdSort() {
            when(crawlerReviewsRepository.findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            crawlerReviewsService.findAll(
                    null,
                    null,
                    1,
                    10,
                    CinemaMovieCrawlerReviewSort.SortBy.REVIEW_STATUS,
                    SortOrder.DESC);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(crawlerReviewsRepository).findAllByFilters(
                    nullable(CinemaResourceType.class),
                    nullable(ReviewStatus.class),
                    pageableCaptor.capture());

            List<Sort.Order> orders = pageableCaptor.getValue().getSort().toList();
            assertEquals(2, orders.size());
            assertEquals("reviewStatus", orders.get(0).getProperty());
            assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
            assertEquals("crawlerReviewId", orders.get(1).getProperty());
            assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
        }
    }

    @Nested
    @DisplayName("createCrawlerReview - 新增爬蟲審核")
    class CreateCrawlerReviewTests {

        @Test
        @DisplayName("Request 為 null 應回傳 400，且不呼叫 Repository")
        void nullRequest() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(null));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("請求內容不可為空", exception.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("爬蟲類型未填應回傳 400")
        void nullCrawlerType() {
            CinemaMovieCrawlerReviewRequest request = request(
                    null, 12, "movieName", null, null, null, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("爬蟲類型不可為空", exception.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("對應資料 ID 未填應回傳 400")
        void nullEntityId() {
            CinemaMovieCrawlerReviewRequest request = request(
                    CinemaResourceType.MOVIE, null, "movieName", null, null, null, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("對應資料ID不可為空", exception.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("欄位名稱未填或只有空白應回傳 400")
        void blankFieldName() {
            CinemaMovieCrawlerReviewRequest nullName = request(
                    CinemaResourceType.MOVIE, 12, null, null, null, null, null);
            CinemaMovieCrawlerReviewRequest blankName = request(
                    CinemaResourceType.MOVIE, 12, "   ", null, null, null, null);

            ResponseStatusException nullNameException = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(nullName));
            ResponseStatusException blankNameException = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(blankName));

            assertEquals(HttpStatus.BAD_REQUEST, nullNameException.getStatusCode());
            assertEquals("欄位名稱不可為空", nullNameException.getReason());
            assertEquals("欄位名稱不可為空", blankNameException.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("相同差異已有待審或已拒絕時應回傳 400，不能再新增")
        void duplicateSameDifference() {
            CinemaMovieCrawlerReviewBean pending = review(2, ReviewStatus.PENDING);
            CinemaMovieCrawlerReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名", null, null);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(review(1, ReviewStatus.APPROVED), pending));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("已有相同差異的審核紀錄，不重複新增", exception.getReason());
            verify(crawlerReviewsRepository, never()).save(any(CinemaMovieCrawlerReviewBean.class));
        }

        @Test
        @DisplayName("爬蟲值與原值相同時應回傳 400")
        void sameValue() {
            CinemaMovieCrawlerReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "同一片名", "同一片名", null, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("爬蟲值與原值相同，無需新增", exception.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("直接新增已拒絕或已接受應回傳 400")
        void rejectNonPendingCreate() {
            CinemaMovieCrawlerReviewRequest request = request(
                    CinemaResourceType.MOVIE,
                    8,
                    "movieName",
                    "新片名",
                    "舊片名",
                    ReviewStatus.REJECTED,
                    REVIEWED_AT);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("新增僅能建立待審核資料", exception.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("沒有相同差異時應新增 PENDING，已接受的歷史不擋新增")
        void shouldCreatePending() {
            CinemaMovieCrawlerReviewRequest request = request(
                    CinemaResourceType.CINEMA,
                    100,
                    "cinema_address",
                    "新地址",
                    "舊地址",
                    null,
                    null);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.CINEMA, 100, "cinema_address"))
                    .thenReturn(List.of(review(1, ReviewStatus.APPROVED)));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> {
                        CinemaMovieCrawlerReviewBean saved = invocation.getArgument(0);
                        saved.setCrawlerReviewId(15);
                        return saved;
                    });

            CinemaMovieCrawlerReviewResponse result = crawlerReviewsService.createCrawlerReview(request);

            ArgumentCaptor<CinemaMovieCrawlerReviewBean> captor = ArgumentCaptor.forClass(CinemaMovieCrawlerReviewBean.class);
            verify(crawlerReviewsRepository).save(captor.capture());
            CinemaMovieCrawlerReviewBean saved = captor.getValue();
            assertEquals(CinemaResourceType.CINEMA, saved.getCrawlerType());
            assertEquals(100, saved.getEntityId());
            assertEquals("cinema_address", saved.getFieldName());
            assertEquals("新地址", saved.getCrawlerValue());
            assertEquals("舊地址", saved.getPreviousValue());
            assertEquals(ReviewStatus.PENDING, saved.getReviewStatus());
            assertEquals(15, result.crawlerReviewId());
        }

        @Test
        @DisplayName("同欄位已有 PENDING 但爬蟲值改變時，應更新該筆而不是再新增")
        void shouldUpdateExistingPending() {
            CinemaMovieCrawlerReviewBean pending = review(2, ReviewStatus.PENDING);
            CinemaMovieCrawlerReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "更新後片名", "舊片名", null, null);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(pending));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaMovieCrawlerReviewResponse result = crawlerReviewsService.createCrawlerReview(request);

            assertEquals(2, result.crawlerReviewId());
            assertEquals("更新後片名", pending.getCrawlerValue());
            assertEquals("舊片名", pending.getPreviousValue());
            assertEquals(ReviewStatus.PENDING, pending.getReviewStatus());
            assertEquals("更新後片名", result.crawlerValue());
            verify(crawlerReviewsRepository).save(pending);
        }
    }

    @Nested
    @DisplayName("submitCrawlerChange - 爬蟲匯入去重")
    class SubmitCrawlerChangeTests {

        @Test
        @DisplayName("相同差異已是 REJECTED 時應略過，不再每天新增")
        void skipRejectedDifference() {
            CinemaMovieCrawlerReviewBean rejected = review(3, ReviewStatus.REJECTED);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(rejected));

            boolean saved = crawlerReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名");

            assertEquals(false, saved);
            verify(crawlerReviewsRepository, never()).save(any(CinemaMovieCrawlerReviewBean.class));
        }

        @Test
        @DisplayName("已接受的電影來源衝突，相同差異不再送審")
        void skipApprovedSourceConflict() {
            CinemaMovieCrawlerReviewBean approved = review(4, ReviewStatus.APPROVED);
            approved.setFieldName(CinemaMovieCrawlerReviewsService.SOURCE_ID);
            approved.setCrawlerValue("fNEW");
            approved.setPreviousValue("fOLD");
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, CinemaMovieCrawlerReviewsService.SOURCE_ID))
                    .thenReturn(List.of(approved));

            boolean saved = crawlerReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, CinemaMovieCrawlerReviewsService.SOURCE_ID, "fNEW", "fOLD");

            assertEquals(false, saved);
            verify(crawlerReviewsRepository, never()).save(any(CinemaMovieCrawlerReviewBean.class));
        }

        @Test
        @DisplayName("已接受的一般欄位不擋相同差異再次送審")
        void approvedMovieNameDoesNotBlockResubmit() {
            CinemaMovieCrawlerReviewBean approved = review(4, ReviewStatus.APPROVED);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(approved));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            boolean saved = crawlerReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名");

            assertEquals(true, saved);
            ArgumentCaptor<CinemaMovieCrawlerReviewBean> captor = ArgumentCaptor.forClass(CinemaMovieCrawlerReviewBean.class);
            verify(crawlerReviewsRepository).save(captor.capture());
            assertEquals(ReviewStatus.PENDING, captor.getValue().getReviewStatus());
            assertEquals("新片名", captor.getValue().getCrawlerValue());
        }

        @Test
        @DisplayName("已接受的來源衝突若新來源不同，仍要再送審")
        void approvedSourceConflictWithNewValue() {
            CinemaMovieCrawlerReviewBean approved = review(4, ReviewStatus.APPROVED);
            approved.setFieldName(CinemaMovieCrawlerReviewsService.SOURCE_ID);
            approved.setCrawlerValue("fNEW");
            approved.setPreviousValue("fOLD");
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, CinemaMovieCrawlerReviewsService.SOURCE_ID))
                    .thenReturn(List.of(approved));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            boolean saved = crawlerReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, CinemaMovieCrawlerReviewsService.SOURCE_ID, "fOTHER", "fOLD");

            assertEquals(true, saved);
            ArgumentCaptor<CinemaMovieCrawlerReviewBean> captor = ArgumentCaptor.forClass(CinemaMovieCrawlerReviewBean.class);
            verify(crawlerReviewsRepository).save(captor.capture());
            assertEquals("fOTHER", captor.getValue().getCrawlerValue());
            assertEquals("fOLD", captor.getValue().getPreviousValue());
            assertEquals(ReviewStatus.PENDING, captor.getValue().getReviewStatus());
        }
    }

    @Nested
    @DisplayName("updateCrawlerReview - 修改爬蟲審核")
    class UpdateCrawlerReviewTests {

        @Test
        @DisplayName("Request 為 null 應回傳 400")
        void nullRequest() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.updateCrawlerReview(7, null));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("請求內容不可為空", exception.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("未帶審查狀態應回傳 400")
        void missingStatus() {
            CinemaMovieCrawlerReviewRequest request = request(
                    null, null, " ", null, null, null, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.updateCrawlerReview(7, request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("審查狀態不可為空", exception.getReason());
            verifyNoInteractions(crawlerReviewsRepository);
        }

        @Test
        @DisplayName("修改不存在的審核應回傳 404")
        void notFound() {
            CinemaMovieCrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.REJECTED, null);
            when(crawlerReviewsRepository.findById(999)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.updateCrawlerReview(999, request));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到爬蟲審核ID：999", exception.getReason());
            verify(crawlerReviewsRepository, never()).save(any(CinemaMovieCrawlerReviewBean.class));
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("接受待審核時應更新正式電影並改成 APPROVED")
        void approvePendingMovie() {
            CinemaMovieCrawlerReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setFieldName(CinemaMovieCrawlerReviewsService.MOVIE_NAME);
            CinemaMovieCrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.APPROVED, null);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(movieService.updateMovie(eq(12), any(MovieRequest.class)))
                    .thenReturn(new MovieResponse(
                            12, null, "新片名", null, null, null, null, null));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaMovieCrawlerReviewResponse result = crawlerReviewsService.updateCrawlerReview(7, request);

            ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
            verify(movieService).updateMovie(eq(12), captor.capture());
            assertEquals("新片名", captor.getValue().movieName());
            assertEquals(null, captor.getValue().imageUrl());
            assertEquals(null, captor.getValue().isVisible());
            assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
            assertNotNull(existing.getReviewedAt());
            assertEquals(ReviewStatus.APPROVED, result.reviewStatus());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("拒絕待審核時應保留正式資料，只改成 REJECTED")
        void rejectPending() {
            CinemaMovieCrawlerReviewBean existing = review(7, ReviewStatus.PENDING);
            CinemaMovieCrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.REJECTED, null);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            crawlerReviewsService.updateCrawlerReview(7, request);

            assertEquals(ReviewStatus.REJECTED, existing.getReviewStatus());
            assertEquals("新片名", existing.getCrawlerValue());
            assertEquals("舊片名", existing.getPreviousValue());
            assertNotNull(existing.getReviewedAt());
            verify(crawlerReviewsRepository).save(existing);
            verifyNoInteractions(movieService);
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("已審核的資料不能直接改狀態")
        void cannotReviewClosedRecord() {
            CinemaMovieCrawlerReviewBean existing = review(7, ReviewStatus.APPROVED);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            CinemaMovieCrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.REJECTED, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.updateCrawlerReview(7, request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("只有待審核資料可以拒絕", exception.getReason());
            assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
            verify(crawlerReviewsRepository, never()).save(any(CinemaMovieCrawlerReviewBean.class));
        }

        @Test
        @DisplayName("重新審核應新增一筆 PENDING，並保留舊紀錄")
        void reopenRejectedReview() {
            CinemaMovieCrawlerReviewBean existing = review(7, ReviewStatus.REJECTED);
            existing.setFieldName(CinemaMovieCrawlerReviewsService.MOVIE_NAME);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, CinemaMovieCrawlerReviewsService.MOVIE_NAME))
                    .thenReturn(List.of(existing));
            when(movieService.findById(12)).thenReturn(new MovieResponse(
                    12, "f1", "目前片名", null, true, null, null, null));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> {
                        CinemaMovieCrawlerReviewBean saved = invocation.getArgument(0);
                        saved.setCrawlerReviewId(21);
                        return saved;
                    });

            CinemaMovieCrawlerReviewResponse result = crawlerReviewsService.updateCrawlerReview(
                    7,
                    request(null, null, null, null, null, ReviewStatus.PENDING, null));

            assertEquals(ReviewStatus.REJECTED, existing.getReviewStatus());
            assertEquals(21, result.crawlerReviewId());
            assertEquals(ReviewStatus.PENDING, result.reviewStatus());
            assertEquals("新片名", result.crawlerValue());
            assertEquals("目前片名", result.previousValue());
            verify(movieService, never()).updateMovie(any(), any());
        }

        @Test
        @DisplayName("接受電影來源衝突時只確認電影存在，不覆蓋正式來源")
        void approveSourceConflictWithoutUpdatingMovie() {
            CinemaMovieCrawlerReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setFieldName(CinemaMovieCrawlerReviewsService.SOURCE_ID);
            existing.setCrawlerValue("fNEW");
            existing.setPreviousValue("fOLD");
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(movieService.findById(12)).thenReturn(new MovieResponse(
                    12, "fOLD", "既有片名", null, true, null, null, null));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaMovieCrawlerReviewResponse result = crawlerReviewsService.updateCrawlerReview(
                    7, request(null, null, null, null, null, ReviewStatus.APPROVED, null));

            assertEquals(ReviewStatus.APPROVED, result.reviewStatus());
            assertNotNull(existing.getReviewedAt());
            verify(movieService).findById(12);
            verify(movieService, never()).updateMovie(any(), any());
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("來源衝突的新來源是空值時不能接受")
        void approveBlankSourceConflict() {
            CinemaMovieCrawlerReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setFieldName(CinemaMovieCrawlerReviewsService.SOURCE_ID);
            existing.setCrawlerValue("  ");
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));

            assertThrows(IllegalArgumentException.class, () -> crawlerReviewsService.updateCrawlerReview(
                    7, request(null, null, null, null, null, ReviewStatus.APPROVED, null)));

            assertEquals(ReviewStatus.PENDING, existing.getReviewStatus());
            verify(movieService, never()).findById(any());
            verify(crawlerReviewsRepository, never()).save(any(CinemaMovieCrawlerReviewBean.class));
        }

        @Test
        @DisplayName("重新審核來源衝突時，原值應改為目前正式來源")
        void reopenSourceConflictUsesCurrentSource() {
            CinemaMovieCrawlerReviewBean existing = review(7, ReviewStatus.REJECTED);
            existing.setFieldName(CinemaMovieCrawlerReviewsService.SOURCE_ID);
            existing.setCrawlerValue("fNEW");
            existing.setPreviousValue("fOLD");
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, CinemaMovieCrawlerReviewsService.SOURCE_ID))
                    .thenReturn(List.of(existing));
            when(movieService.findById(12)).thenReturn(new MovieResponse(
                    12, "fCURRENT", "目前片名", null, true, null, null, null));
            when(crawlerReviewsRepository.save(any(CinemaMovieCrawlerReviewBean.class)))
                    .thenAnswer(invocation -> {
                        CinemaMovieCrawlerReviewBean saved = invocation.getArgument(0);
                        saved.setCrawlerReviewId(22);
                        return saved;
                    });

            CinemaMovieCrawlerReviewResponse result = crawlerReviewsService.updateCrawlerReview(
                    7, request(null, null, null, null, null, ReviewStatus.PENDING, null));

            assertEquals(ReviewStatus.REJECTED, existing.getReviewStatus());
            assertEquals(22, result.crawlerReviewId());
            assertEquals(ReviewStatus.PENDING, result.reviewStatus());
            assertEquals("fNEW", result.crawlerValue());
            assertEquals("fCURRENT", result.previousValue());
            verify(movieService, never()).updateMovie(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteCrawlerReview - 刪除爬蟲審核")
    class DeleteCrawlerReviewTests {

        @Test
        @DisplayName("存在的 ID 應刪除該筆審核")
        void shouldDelete() {
            when(crawlerReviewsRepository.existsById(7)).thenReturn(true);

            crawlerReviewsService.deleteCrawlerReview(7);

            verify(crawlerReviewsRepository).deleteById(7);
        }

        @Test
        @DisplayName("不存在的 ID 應回傳 404，不能刪除")
        void notFound() {
            when(crawlerReviewsRepository.existsById(999)).thenReturn(false);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.deleteCrawlerReview(999));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到爬蟲審核ID：999", exception.getReason());
            verify(crawlerReviewsRepository, never()).deleteById(any());
        }
    }
}
