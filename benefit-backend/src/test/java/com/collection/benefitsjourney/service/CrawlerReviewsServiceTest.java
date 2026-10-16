package com.collection.benefitsjourney.service;

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

import com.collection.benefitsjourney.domain.CrawlerReviewBean;
import com.collection.benefitsjourney.dto.CrawlerReviewRequest;
import com.collection.benefitsjourney.dto.CrawlerReviewResponse;
import com.collection.benefitsjourney.dto.MovieRequest;
import com.collection.benefitsjourney.dto.MovieResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaResourceType;
import com.collection.benefitsjourney.enums.CrawlerReviewSort;
import com.collection.benefitsjourney.enums.ReviewStatus;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CrawlerReviewsRepository;

@ExtendWith(MockitoExtension.class)
class CrawlerReviewsServiceTest {

    @Mock
    private CrawlerReviewsRepository crawlerReviewsRepository;

    @Mock
    private CinemaService cinemaService;

    @Mock
    private MovieService movieService;

    @InjectMocks
    private CrawlerReviewsService crawlerReviewsService;

    private static final LocalDateTime REVIEWED_AT = LocalDateTime.of(2026, 10, 9, 11, 0);

    private CrawlerReviewBean review(Integer id, ReviewStatus status) {
        CrawlerReviewBean review = new CrawlerReviewBean();
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

    private CrawlerReviewRequest request(
            CinemaResourceType crawlerType,
            Integer entityId,
            String fieldName,
            String crawlerValue,
            String previousValue,
            ReviewStatus reviewStatus,
            LocalDateTime reviewedAt) {
        return new CrawlerReviewRequest(
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
            CrawlerReviewBean bean = review(7, ReviewStatus.PENDING);
            when(crawlerReviewsRepository.findAllByFilters(
                    eq(CinemaResourceType.MOVIE),
                    eq(ReviewStatus.PENDING),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(bean),
                            PageRequest.of(2, 20),
                            41));

            PageResponse<CrawlerReviewResponse> result = crawlerReviewsService.findAll(
                    CinemaResourceType.MOVIE,
                    ReviewStatus.PENDING,
                    3,
                    20,
                    CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID,
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
            CrawlerReviewResponse response = result.content().get(0);
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

            PageResponse<CrawlerReviewResponse> result = crawlerReviewsService.findAll(
                    null,
                    null,
                    1,
                    10,
                    CrawlerReviewSort.SortBy.CRAWLER_REVIEW_ID,
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
                    CrawlerReviewSort.SortBy.REVIEW_STATUS,
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
            CrawlerReviewRequest request = request(
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
            CrawlerReviewRequest request = request(
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
            CrawlerReviewRequest nullName = request(
                    CinemaResourceType.MOVIE, 12, null, null, null, null, null);
            CrawlerReviewRequest blankName = request(
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
            CrawlerReviewBean pending = review(2, ReviewStatus.PENDING);
            CrawlerReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名", null, null);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(review(1, ReviewStatus.APPROVED), pending));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.createCrawlerReview(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("已有相同差異的審核紀錄，不重複新增", exception.getReason());
            verify(crawlerReviewsRepository, never()).save(any(CrawlerReviewBean.class));
        }

        @Test
        @DisplayName("爬蟲值與原值相同時應回傳 400")
        void sameValue() {
            CrawlerReviewRequest request = request(
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
            CrawlerReviewRequest request = request(
                    CinemaResourceType.BENEFIT,
                    8,
                    "benefitName",
                    "新特典",
                    "舊特典",
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
            CrawlerReviewRequest request = request(
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
            when(crawlerReviewsRepository.save(any(CrawlerReviewBean.class)))
                    .thenAnswer(invocation -> {
                        CrawlerReviewBean saved = invocation.getArgument(0);
                        saved.setCrawlerReviewId(15);
                        return saved;
                    });

            CrawlerReviewResponse result = crawlerReviewsService.createCrawlerReview(request);

            ArgumentCaptor<CrawlerReviewBean> captor = ArgumentCaptor.forClass(CrawlerReviewBean.class);
            verify(crawlerReviewsRepository).save(captor.capture());
            CrawlerReviewBean saved = captor.getValue();
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
            CrawlerReviewBean pending = review(2, ReviewStatus.PENDING);
            CrawlerReviewRequest request = request(
                    CinemaResourceType.MOVIE, 12, "movieName", "更新後片名", "舊片名", null, null);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(pending));
            when(crawlerReviewsRepository.save(any(CrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CrawlerReviewResponse result = crawlerReviewsService.createCrawlerReview(request);

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
            CrawlerReviewBean rejected = review(3, ReviewStatus.REJECTED);
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, "movieName"))
                    .thenReturn(List.of(rejected));

            boolean saved = crawlerReviewsService.submitCrawlerChange(
                    CinemaResourceType.MOVIE, 12, "movieName", "新片名", "舊片名");

            assertEquals(false, saved);
            verify(crawlerReviewsRepository, never()).save(any(CrawlerReviewBean.class));
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
            CrawlerReviewRequest request = request(
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
            CrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.REJECTED, null);
            when(crawlerReviewsRepository.findById(999)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.updateCrawlerReview(999, request));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到爬蟲審核 ID：999", exception.getReason());
            verify(crawlerReviewsRepository, never()).save(any(CrawlerReviewBean.class));
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("接受待審核時應更新正式電影並改成 APPROVED")
        void approvePendingMovie() {
            CrawlerReviewBean existing = review(7, ReviewStatus.PENDING);
            existing.setFieldName(CrawlerReviewsService.MOVIE_NAME);
            CrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.APPROVED, null);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(movieService.updateMovie(eq(12), any(MovieRequest.class)))
                    .thenReturn(new MovieResponse(
                            12, null, "新片名", null, null, null, null, null));
            when(crawlerReviewsRepository.save(any(CrawlerReviewBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CrawlerReviewResponse result = crawlerReviewsService.updateCrawlerReview(7, request);

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
            CrawlerReviewBean existing = review(7, ReviewStatus.PENDING);
            CrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.REJECTED, null);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(crawlerReviewsRepository.save(any(CrawlerReviewBean.class)))
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
            CrawlerReviewBean existing = review(7, ReviewStatus.APPROVED);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            CrawlerReviewRequest request = request(
                    null, null, null, null, null, ReviewStatus.REJECTED, null);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> crawlerReviewsService.updateCrawlerReview(7, request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("只有待審核資料可以拒絕", exception.getReason());
            assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
            verify(crawlerReviewsRepository, never()).save(any(CrawlerReviewBean.class));
        }

        @Test
        @DisplayName("重新審核應新增一筆 PENDING，並保留舊紀錄")
        void reopenRejectedReview() {
            CrawlerReviewBean existing = review(7, ReviewStatus.REJECTED);
            existing.setFieldName(CrawlerReviewsService.MOVIE_NAME);
            when(crawlerReviewsRepository.findById(7)).thenReturn(Optional.of(existing));
            when(crawlerReviewsRepository.findByCrawlerTypeAndEntityIdAndFieldName(
                    CinemaResourceType.MOVIE, 12, CrawlerReviewsService.MOVIE_NAME))
                    .thenReturn(List.of(existing));
            when(movieService.findById(12)).thenReturn(new MovieResponse(
                    12, "f1", "目前片名", null, true, null, null, null));
            when(crawlerReviewsRepository.save(any(CrawlerReviewBean.class)))
                    .thenAnswer(invocation -> {
                        CrawlerReviewBean saved = invocation.getArgument(0);
                        saved.setCrawlerReviewId(21);
                        return saved;
                    });

            CrawlerReviewResponse result = crawlerReviewsService.updateCrawlerReview(
                    7,
                    request(null, null, null, null, null, ReviewStatus.PENDING, null));

            assertEquals(ReviewStatus.REJECTED, existing.getReviewStatus());
            assertEquals(21, result.crawlerReviewId());
            assertEquals(ReviewStatus.PENDING, result.reviewStatus());
            assertEquals("新片名", result.crawlerValue());
            assertEquals("目前片名", result.previousValue());
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
            assertEquals("找不到爬蟲審核 ID：999", exception.getReason());
            verify(crawlerReviewsRepository, never()).deleteById(any());
        }
    }
}
