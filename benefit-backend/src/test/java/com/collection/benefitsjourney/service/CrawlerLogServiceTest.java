package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.collection.benefitsjourney.domain.CrawlerLogBean;
import com.collection.benefitsjourney.dto.CrawlerLogRequest;
import com.collection.benefitsjourney.dto.CrawlerLogResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CrawlerLogSort;
import com.collection.benefitsjourney.enums.CrawlerStatus;
import com.collection.benefitsjourney.enums.CrawlerType;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CrawlerLogRepository;

@ExtendWith(MockitoExtension.class)
class CrawlerLogServiceTest {

    @Mock
    private CrawlerLogRepository crawlerLogRepository;

    @InjectMocks
    private CrawlerLogService crawlerLogService;

    private static final LocalDateTime EXECUTED_AT = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime FINISHED_AT = LocalDateTime.of(2026, 10, 20, 10, 1, 30);

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("依執行時間倒序，並以紀錄 ID 作為次排序")
        void sortByExecutedAtDesc() {
            CrawlerLogBean saved = log(3);
            when(crawlerLogRepository.findAllByFilters(isNull(), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of(saved), Pageable.ofSize(20), 1));

            PageResponse<CrawlerLogResponse> result = crawlerLogService.findAll(
                    null, null, 1, 20, CrawlerLogSort.SortBy.EXECUTED_AT, SortOrder.DESC);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(crawlerLogRepository).findAllByFilters(isNull(), isNull(), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertEquals(0, pageable.getPageNumber());
            assertEquals(20, pageable.getPageSize());
            List<Sort.Order> orders = pageable.getSort().toList();
            assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
            assertEquals("executedAt", orders.get(0).getProperty());
            assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
            assertEquals("crawlerLogId", orders.get(1).getProperty());
            assertEquals(1, result.page());
            assertEquals(3, result.content().get(0).crawlerLogId());
        }
    }

    @Nested
    @DisplayName("createCrawlerLog")
    class CreateTests {

        @Test
        @DisplayName("應寫入裁切後的時間與訊息")
        void create() {
            when(crawlerLogRepository.save(any())).thenAnswer(invocation -> {
                CrawlerLogBean saved = invocation.getArgument(0);
                saved.setCrawlerLogId(8);
                return saved;
            });

            CrawlerLogResponse result = crawlerLogService.createCrawlerLog(new CrawlerLogRequest(
                    CrawlerType.CINEMA,
                    CrawlerStatus.PARTIAL,
                    5, 1, 2, 1, 1,
                    "影城同步完成",
                    EXECUTED_AT.withNano(123),
                    FINISHED_AT));

            ArgumentCaptor<CrawlerLogBean> captor = ArgumentCaptor.forClass(CrawlerLogBean.class);
            verify(crawlerLogRepository).save(captor.capture());
            CrawlerLogBean saved = captor.getValue();
            assertEquals(CrawlerType.CINEMA, saved.getCrawlerType());
            assertEquals(CrawlerStatus.PARTIAL, saved.getCrawlerStatus());
            assertEquals("影城同步完成", saved.getMessage());
            assertEquals(EXECUTED_AT, saved.getExecutedAt());
            assertEquals(8, result.crawlerLogId());
        }
    }

    private static CrawlerLogBean log(Integer id) {
        CrawlerLogBean log = new CrawlerLogBean();
        log.setCrawlerLogId(id);
        log.setCrawlerType(CrawlerType.MOVIE);
        log.setCrawlerStatus(CrawlerStatus.SUCCESS);
        log.setFetchedCount(10);
        log.setCreatedCount(2);
        log.setUpdatedCount(3);
        log.setSkippedCount(1);
        log.setPendingReviewCount(4);
        log.setMessage("電影同步完成");
        log.setExecutedAt(EXECUTED_AT);
        log.setFinishedAt(FINISHED_AT);
        return log;
    }
}
