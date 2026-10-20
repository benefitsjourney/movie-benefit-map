package com.collection.benefitsjourney.service.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaSort;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CinemaRepository;

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;

    @InjectMocks
    private CinemaService cinemaService;

    private CinemaRequest request() {
        return mock(CinemaRequest.class, invocation -> {
            if (invocation.getMethod().getReturnType() == Boolean.class) {
                return null;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    private CinemaBean cinema(Integer id, String sourceId, String name) {
        CinemaBean cinema = new CinemaBean();
        cinema.setCinemaId(id);
        cinema.setSourceId(sourceId);
        cinema.setCinemaName(name);
        cinema.setCounty(County.TAIPEI);
        cinema.setIsActive(true);
        cinema.setIsVisible(true);
        return cinema;
    }

    @Nested
    @DisplayName("findAll - 查詢、分頁與排序")
    class FindAllTests {

        @Test
        @DisplayName("查詢參數應傳到 Repository，頁碼應從 1-based 轉 0-based")
        void shouldPassFiltersAndPaging() {
            CinemaBean bean = cinema(100, "T001", "信義威秀");
            when(cinemaRepository.findAllByFilters(
                    any(String.class), eq(County.TAIPEI), eq(true), eq(false),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(bean)));

            PageResponse<CinemaResponse> result = cinemaService.findAll(
                    "威秀", County.TAIPEI, true, false,
                    3, 20, CinemaSort.SortBy.CINEMA_ID, SortOrder.ASC);

            assertNotNull(result);

            ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(cinemaRepository).findAllByFilters(
                    keywordCaptor.capture(), eq(County.TAIPEI), eq(true), eq(false),
                    pageableCaptor.capture());

            // 不綁死 LikeKeywords 內部的跳脫規則，只驗證關鍵字被保留。
            assertTrue(keywordCaptor.getValue().contains("威秀"));
            Pageable pageable = pageableCaptor.getValue();
            assertEquals(2, pageable.getPageNumber());
            assertEquals(20, pageable.getPageSize());
            Sort.Order order = pageable.getSort().getOrderFor("cinemaId");
            assertNotNull(order);
            assertEquals(Sort.Direction.ASC, order.getDirection());
        }

        @Test
        @DisplayName("縣市排序應由北到南，同縣市再依 cinemaId")
        void shouldSortCountyFromNorthToSouth() {
            when(cinemaRepository.findAllByFilters(
                    nullable(String.class), nullable(County.class),
                    nullable(Boolean.class), nullable(Boolean.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            PageResponse<CinemaResponse> result = cinemaService.findAll(
                    null, null, null, null,
                    1, 10, CinemaSort.SortBy.COUNTY, SortOrder.ASC);

            assertNotNull(result);
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(cinemaRepository).findAllByFilters(
                    nullable(String.class), nullable(County.class),
                    nullable(Boolean.class), nullable(Boolean.class),
                    captor.capture());

            Pageable pageable = captor.getValue();
            assertEquals(0, pageable.getPageNumber());
            assertEquals(10, pageable.getPageSize());
            String countyOrder = pageable.getSort().stream().findFirst().orElseThrow().getProperty();
            assertEquals(Sort.Direction.ASC, pageable.getSort().stream().findFirst().orElseThrow().getDirection());
            assertTrue(countyOrder.startsWith("(CASE "));
            assertTrue(countyOrder.endsWith("END)"));
            assertTrue(countyOrder.contains("KEELUNG THEN " + County.KEELUNG.northToSouth()));
            assertTrue(countyOrder.contains("PINGTUNG THEN " + County.PINGTUNG.northToSouth()));
            assertTrue(County.HSINCHU.northToSouth() < County.YILAN.northToSouth());
            assertTrue(County.YILAN.northToSouth() < County.MIAOLI.northToSouth());
            assertEquals(5, County.YILAN.northToSouth());
            assertTrue(County.TAITUNG.northToSouth() < County.UNKNOWN.northToSouth());
            assertEquals(Sort.Direction.ASC,
                    pageable.getSort().getOrderFor("cinemaId").getDirection());
        }
    }

    @Nested
    @DisplayName("findById - 單筆查詢")
    class FindByIdTests {

        @Test
        @DisplayName("存在的 ID 應回傳轉換後的 CinemaResponse")
        void shouldReturnCinema() {
            CinemaBean bean = cinema(100, "T001", "信義威秀");
            when(cinemaRepository.findById(100)).thenReturn(Optional.of(bean));

            CinemaResponse result = cinemaService.findById(100);

            assertNotNull(result);
            verify(cinemaRepository).findById(100);
        }

        @Test
        @DisplayName("不存在的 ID 應回傳 404")
        void shouldThrowNotFound() {
            when(cinemaRepository.findById(999)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class, () -> cinemaService.findById(999));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到影城 ID：999", exception.getReason());
        }
    }

    @Nested
    @DisplayName("createCinema - 新增影城")
    class CreateCinemaTests {

        @Test
        @DisplayName("Request 為 null 應回傳 400，且不呼叫 Repository")
        void nullRequest() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class, () -> cinemaService.createCinema(null));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("請求內容不可為空", exception.getReason());
            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("名稱未填應回傳 400")
        void nullName() {
            CinemaRequest request = request();

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class, () -> cinemaService.createCinema(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("影城名稱不可為空", exception.getReason());
            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("名稱只含空白應回傳 400")
        void blankName() {
            CinemaRequest request = request();
            when(request.cinemaName()).thenReturn("   ");

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class, () -> cinemaService.createCinema(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("影城名稱不可為空", exception.getReason());
            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("縣市未填應回傳 400")
        void missingCounty() {
            CinemaRequest request = request();
            when(request.cinemaName()).thenReturn("測試影城");

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class, () -> cinemaService.createCinema(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("縣市不可為空", exception.getReason());
            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("來源 ID 與現有影城重複應回傳 409，不能儲存")
        void duplicateSourceId() {
            CinemaRequest request = request();
            when(request.cinemaName()).thenReturn("測試影城");
            when(request.county()).thenReturn(County.TAIPEI);
            when(request.sourceId()).thenReturn("T001");
            when(cinemaRepository.findBySourceId("T001"))
                    .thenReturn(Optional.of(cinema(100, "T001", "既有影城")));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class, () -> cinemaService.createCinema(request));

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("來源 ID 已存在：T001", exception.getReason());
            verify(cinemaRepository, never()).save(any(CinemaBean.class));
        }

        @Test
        @DisplayName("未提供 isActive 與 isVisible 時，新增後皆應為 true")
        void shouldSetDefaultFlags() {
            CinemaRequest request = request();
            when(request.cinemaName()).thenReturn("測試影城");
            when(request.county()).thenReturn(County.TAIPEI);
            when(cinemaRepository.save(any(CinemaBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaResponse result = cinemaService.createCinema(request);

            assertNotNull(result);
            ArgumentCaptor<CinemaBean> captor = ArgumentCaptor.forClass(CinemaBean.class);
            verify(cinemaRepository).save(captor.capture());
            CinemaBean saved = captor.getValue();
            assertEquals("測試影城", saved.getCinemaName());
            assertEquals(County.TAIPEI, saved.getCounty());
            assertEquals(Boolean.TRUE, saved.getIsActive());
            assertEquals(Boolean.TRUE, saved.getIsVisible());
            verify(cinemaRepository, never()).findBySourceId(any(String.class));
        }

        @Test
        @DisplayName("完整 Request 欄位應正確儲存，明確傳入 false 不應被覆寫")
        void shouldSaveAllFields() {
            CinemaRequest request = request();
            when(request.sourceId()).thenReturn("T002");
            when(request.sourceUrl()).thenReturn("https://www.atmovies.com.tw/showtime/T002/");
            when(request.cinemaName()).thenReturn("京站威秀");
            when(request.county()).thenReturn(County.TAIPEI);
            when(request.cinemaAddress()).thenReturn("台北市大同區測試路1號");
            when(request.latitude()).thenReturn(new BigDecimal("25.048785"));
            when(request.longitude()).thenReturn(new BigDecimal("121.518569"));
            when(request.officialWebsiteUrl()).thenReturn("https://example.com");
            when(request.isActive()).thenReturn(false);
            when(request.isVisible()).thenReturn(false);
            when(cinemaRepository.findBySourceId("T002")).thenReturn(Optional.empty());
            when(cinemaRepository.save(any(CinemaBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            cinemaService.createCinema(request);

            ArgumentCaptor<CinemaBean> captor = ArgumentCaptor.forClass(CinemaBean.class);
            verify(cinemaRepository).save(captor.capture());
            CinemaBean saved = captor.getValue();
            assertEquals("T002", saved.getSourceId());
            assertEquals("https://www.atmovies.com.tw/showtime/T002/", saved.getSourceUrl());
            assertEquals("京站威秀", saved.getCinemaName());
            assertEquals("台北市大同區測試路1號", saved.getCinemaAddress());
            assertEquals(0, new BigDecimal("25.048785").compareTo(saved.getLatitude()));
            assertEquals(0, new BigDecimal("121.518569").compareTo(saved.getLongitude()));
            assertEquals("https://example.com", saved.getOfficialWebsiteUrl());
            assertEquals(Boolean.FALSE, saved.getIsActive());
            assertEquals(Boolean.FALSE, saved.getIsVisible());
        }
    }

    @Nested
    @DisplayName("updateCinema - 修改影城")
    class UpdateCinemaTests {

        @Test
        @DisplayName("Request 為 null 應回傳 400")
        void nullRequest() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaService.updateCinema(100, null));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("請求內容不可為空", exception.getReason());
            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("修改名稱為空白應回傳 400")
        void blankName() {
            CinemaRequest request = request();
            when(request.cinemaName()).thenReturn(" ");

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaService.updateCinema(100, request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("影城名稱不可為空", exception.getReason());
            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("修改不存在的影城應回傳 404")
        void notFound() {
            CinemaRequest request = request();
            when(cinemaRepository.findById(999)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaService.updateCinema(999, request));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("找不到影城ID：999", exception.getReason());
            verify(cinemaRepository, never()).save(any(CinemaBean.class));
        }

        @Test
        @DisplayName("部分修改只更新帶入欄位，其餘欄位保持原值")
        void shouldOnlyUpdateProvidedFields() {
            CinemaBean existing = cinema(100, "T001", "原本影城");
            existing.setCinemaAddress("原本地址");
            existing.setIsVisible(false);

            CinemaRequest request = request();
            when(request.cinemaName()).thenReturn("改名後影城");
            when(cinemaRepository.findById(100)).thenReturn(Optional.of(existing));
            when(cinemaRepository.save(any(CinemaBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaResponse result = cinemaService.updateCinema(100, request);

            assertNotNull(result);
            ArgumentCaptor<CinemaBean> captor = ArgumentCaptor.forClass(CinemaBean.class);
            verify(cinemaRepository).save(captor.capture());
            CinemaBean saved = captor.getValue();
            assertSame(existing, saved);
            assertEquals("改名後影城", saved.getCinemaName());
            assertEquals("T001", saved.getSourceId());
            assertEquals("原本地址", saved.getCinemaAddress());
            assertEquals(Boolean.TRUE, saved.getIsActive());
            assertEquals(Boolean.FALSE, saved.getIsVisible());
            verify(cinemaRepository, never()).findBySourceId(any(String.class));
        }

        @Test
        @DisplayName("sourceId 和自己相同時可正常修改，不算重複")
        void sameSourceIdShouldBeAllowed() {
            CinemaBean existing = cinema(100, "T001", "原本影城");
            CinemaRequest request = request();
            when(request.sourceId()).thenReturn("T001");
            when(cinemaRepository.findById(100)).thenReturn(Optional.of(existing));
            when(cinemaRepository.findBySourceId("T001"))
                    .thenReturn(Optional.of(existing));
            when(cinemaRepository.save(any(CinemaBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CinemaResponse result = cinemaService.updateCinema(100, request);

            assertNotNull(result);
            verify(cinemaRepository).save(existing);
        }

        @Test
        @DisplayName("sourceId 被另一間影城使用時應回傳 409")
        void duplicateSourceIdShouldConflict() {
            CinemaBean existing = cinema(100, "T001", "目前影城");
            CinemaBean other = cinema(200, "T002", "其他影城");
            CinemaRequest request = request();
            when(request.sourceId()).thenReturn("T002");
            when(cinemaRepository.findById(100)).thenReturn(Optional.of(existing));
            when(cinemaRepository.findBySourceId("T002"))
                    .thenReturn(Optional.of(other));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaService.updateCinema(100, request));

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("來源ID已存在：T002", exception.getReason());
            verify(cinemaRepository, never()).save(any(CinemaBean.class));
        }

        @Test
        @DisplayName("修改經緯度、縣市、網站、啟用與顯示狀態")
        void shouldUpdateEveryOptionalField() {
            CinemaBean existing = cinema(100, "T001", "原本影城");
            CinemaRequest request = request();
            when(request.cinemaAddress()).thenReturn("新地址");
            when(request.latitude()).thenReturn(new BigDecimal("25.01"));
            when(request.longitude()).thenReturn(new BigDecimal("121.46"));
            when(request.officialWebsiteUrl()).thenReturn("https://new.example.com");
            when(request.sourceUrl()).thenReturn("https://www.atmovies.com.tw/showtime/t02a01/");
            when(request.county()).thenReturn(County.NEW_TAIPEI);
            when(request.isActive()).thenReturn(false);
            when(request.isVisible()).thenReturn(false);
            when(cinemaRepository.findById(100)).thenReturn(Optional.of(existing));
            when(cinemaRepository.save(any(CinemaBean.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            cinemaService.updateCinema(100, request);

            assertEquals("新地址", existing.getCinemaAddress());
            assertEquals(0, new BigDecimal("25.01").compareTo(existing.getLatitude()));
            assertEquals(0, new BigDecimal("121.46").compareTo(existing.getLongitude()));
            assertEquals("https://new.example.com", existing.getOfficialWebsiteUrl());
            assertEquals("https://www.atmovies.com.tw/showtime/t02a01/", existing.getSourceUrl());
            assertEquals(County.NEW_TAIPEI, existing.getCounty());
            assertEquals(Boolean.FALSE, existing.getIsActive());
            assertEquals(Boolean.FALSE, existing.getIsVisible());
        }
    }

    @Nested
    @DisplayName("findBySourceId - 根據來源 ID 查詢")
    class FindBySourceIdTests {

        @Test
        @DisplayName("sourceId 為 null 或空白應直接回傳 empty，不能查詢 Repository")
        void blankSourceId() {
            assertTrue(cinemaService.findBySourceId(null).isEmpty());
            assertTrue(cinemaService.findBySourceId("").isEmpty());
            assertTrue(cinemaService.findBySourceId("   ").isEmpty());
            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("來源 ID 存在時應回傳 CinemaResponse")
        void shouldReturnCinema() {
            when(cinemaRepository.findBySourceId("T001"))
                    .thenReturn(Optional.of(cinema(100, "T001", "信義威秀")));

            Optional<CinemaResponse> result = cinemaService.findBySourceId("T001");

            assertTrue(result.isPresent());
            verify(cinemaRepository).findBySourceId("T001");
        }

        @Test
        @DisplayName("來源 ID 不存在時應回傳 empty")
        void shouldReturnEmpty() {
            when(cinemaRepository.findBySourceId("UNKNOWN"))
                    .thenReturn(Optional.empty());

            Optional<CinemaResponse> result = cinemaService.findBySourceId("UNKNOWN");

            assertFalse(result.isPresent());
            verify(cinemaRepository).findBySourceId("UNKNOWN");
        }
    }
}
