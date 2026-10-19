package com.collection.benefitsjourney.service.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMapResponse;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.repository.CinemaRepository;

@ExtendWith(MockitoExtension.class)
class CinemaMapServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;

    @InjectMocks
    private CinemaMapService cinemaMapService;

    private CinemaBean cinema(
            Integer id,
            String name,
            double latitude,
            double longitude) {

        CinemaBean cinema = new CinemaBean();
        cinema.setCinemaId(id);
        cinema.setCinemaName(name);
        cinema.setCounty(County.TAIPEI);
        cinema.setLatitude(BigDecimal.valueOf(latitude));
        cinema.setLongitude(BigDecimal.valueOf(longitude));
        cinema.setIsActive(true);
        cinema.setIsVisible(true);

        return cinema;
    }

    @Nested
    @DisplayName("findNearby - 全台模式")
    class NationwideTests {

        @Test
        @DisplayName("未提供座標時應回傳全部影城，並依影城 ID 升冪排序")
        void shouldReturnAllCinemasSortedById() {
            CinemaBean first = cinema(3, "影城C", 25.03, 121.5);
            CinemaBean second = cinema(1, "影城A", 25.01, 121.5);
            CinemaBean third = cinema(2, "影城B", 25.02, 121.5);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    eq(County.TAIPEI)))
                    .thenReturn(List.of(first, second, third));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, County.TAIPEI, null, null, null);

            assertNotNull(result);
            assertEquals(3, result.total());

            assertNull(result.latitude());
            assertNull(result.longitude());
            assertNull(result.radius());

            assertEquals(3, result.cinemas().size());

            assertEquals(1, result.cinemas().get(0).cinemaId());
            assertEquals(2, result.cinemas().get(1).cinemaId());
            assertEquals(3, result.cinemas().get(2).cinemaId());

            // 全台模式不計算距離
            assertTrue(result.cinemas().stream()
                    .allMatch(cinema -> cinema.distance() == null));

            verify(cinemaRepository).findAllForMap(
                    nullable(String.class),
                    eq(County.TAIPEI));
        }

        @Test
        @DisplayName("關鍵字與縣市篩選條件應正確傳入 Repository")
        void shouldPassFilters() {
            when(cinemaRepository.findAllForMap(
                    any(String.class),
                    eq(County.TAIPEI)))
                    .thenReturn(List.of());

            CinemaMapResponse result = cinemaMapService.findNearby(
                    "威秀", County.TAIPEI, null, null, null);

            assertNotNull(result);
            assertEquals(0, result.total());
            assertTrue(result.cinemas().isEmpty());

            ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);

            verify(cinemaRepository).findAllForMap(
                    keywordCaptor.capture(),
                    eq(County.TAIPEI));

            // 不綁死 LikeKeywords 內部的跳脫規則
            assertTrue(keywordCaptor.getValue().contains("威秀"));
        }

        @Test
        @DisplayName("沒有符合條件的影城時應回傳空清單")
        void shouldReturnEmptyList() {
            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of());

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, null, null, null);

            assertNotNull(result);
            assertEquals(0, result.total());
            assertTrue(result.cinemas().isEmpty());

            assertNull(result.latitude());
            assertNull(result.longitude());
            assertNull(result.radius());
        }
    }

    @Nested
    @DisplayName("findNearby - 附近模式")
    class NearbyTests {

        @Test
        @DisplayName("未指定半徑時應使用預設 5000 公尺")
        void shouldUseDefaultRadius() {
            CinemaBean center = cinema(1, "中心影城", 25.0, 121.0);
            CinemaBean nearby = cinema(2, "附近影城", 25.01, 121.0);
            CinemaBean far = cinema(3, "遠方影城", 25.1, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(far, nearby, center));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, null);

            assertNotNull(result);
            assertEquals(2, result.total());
            assertEquals(25.0, result.latitude());
            assertEquals(121.0, result.longitude());
            assertEquals(5000.0, result.radius());

            assertEquals(1, result.cinemas().get(0).cinemaId());
            assertEquals(2, result.cinemas().get(1).cinemaId());
        }

        @Test
        @DisplayName("指定半徑時應只回傳範圍內的影城")
        void shouldFilterByCustomRadius() {
            CinemaBean center = cinema(1, "中心影城", 25.0, 121.0);
            CinemaBean nearby = cinema(2, "附近影城", 25.005, 121.0);
            CinemaBean far = cinema(3, "遠方影城", 25.02, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(center, nearby, far));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 1000.0);

            assertNotNull(result);
            assertEquals(2, result.total());
            assertEquals(1000.0, result.radius());

            assertEquals(1, result.cinemas().get(0).cinemaId());
            assertEquals(2, result.cinemas().get(1).cinemaId());

            assertTrue(result.cinemas().stream()
                    .allMatch(cinema -> cinema.distance() <= 1000.0));
        }

        @Test
        @DisplayName("影城與搜尋中心相同座標時距離應為 0")
        void sameLocationShouldHaveZeroDistance() {
            CinemaBean center = cinema(1, "中心影城", 25.0, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(center));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 1000.0);

            assertEquals(1, result.total());
            assertEquals(0.0, result.cinemas().get(0).distance());
        }

        @Test
        @DisplayName("Haversine 距離應以公尺計算並四捨五入至小數一位")
        void shouldCalculateAndRoundDistance() {
            // 緯度差 0.01 度，距離約為 1111.9 公尺
            CinemaBean target = cinema(1, "測試影城", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(target));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0);

            assertEquals(1, result.total());

            Double distance = result.cinemas().get(0).distance();

            assertNotNull(distance);
            assertEquals(1111.9, distance, 0.1);

            // 四捨五入後最多保留一位小數
            assertEquals(
                    Math.round(distance * 10) / 10.0,
                    distance);
        }

        @Test
        @DisplayName("附近影城應依原始距離由近到遠排序")
        void shouldSortByDistanceAscending() {
            CinemaBean far = cinema(1, "遠方影城", 25.03, 121.0);
            CinemaBean center = cinema(2, "中心影城", 25.0, 121.0);
            CinemaBean near = cinema(3, "附近影城", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(far, near, center));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0);

            assertEquals(3, result.total());

            assertEquals(2, result.cinemas().get(0).cinemaId());
            assertEquals(3, result.cinemas().get(1).cinemaId());
            assertEquals(1, result.cinemas().get(2).cinemaId());

            assertTrue(
                    result.cinemas().get(0).distance() <= result.cinemas().get(1).distance());

            assertTrue(
                    result.cinemas().get(1).distance() <= result.cinemas().get(2).distance());
        }

        @Test
        @DisplayName("距離相同時應依影城 ID 升冪排序")
        void sameDistanceShouldSortById() {
            CinemaBean second = cinema(2, "影城B", 25.01, 121.0);
            CinemaBean first = cinema(1, "影城A", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(second, first));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0);

            assertEquals(2, result.total());

            assertEquals(1, result.cinemas().get(0).cinemaId());
            assertEquals(2, result.cinemas().get(1).cinemaId());

            assertEquals(
                    result.cinemas().get(0).distance(),
                    result.cinemas().get(1).distance());
        }

        @Test
        @DisplayName("排序應使用原始距離，而非四捨五入後的距離")
        void shouldSortBeforeRounding() {
            // 兩個距離都會四捨五入為 1.0 公尺，
            // 但 ID 2 的實際距離比 ID 1 更近。
            CinemaBean farther = cinema(1, "稍遠影城", 0.00000904, 0.0);
            CinemaBean nearer = cinema(2, "稍近影城", 0.00000900, 0.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(farther, nearer));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 0.0, 0.0, 100.0);

            assertEquals(2, result.total());

            // 應以原始距離排序，ID 2 比 ID 1 近
            assertEquals(2, result.cinemas().get(0).cinemaId());
            assertEquals(1, result.cinemas().get(1).cinemaId());

            // 顯示的距離四捨五入後相同
            assertEquals(1.0, result.cinemas().get(0).distance());
            assertEquals(1.0, result.cinemas().get(1).distance());
        }

        @Test
        @DisplayName("沒有影城位於搜尋半徑內時應回傳空清單")
        void shouldReturnEmptyWhenNoCinemaInRadius() {
            CinemaBean far = cinema(1, "遠方影城", 25.1, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(far));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 1000.0);

            assertNotNull(result);
            assertEquals(0, result.total());
            assertTrue(result.cinemas().isEmpty());

            // 即使結果為空，仍應保留搜尋參數
            assertEquals(25.0, result.latitude());
            assertEquals(121.0, result.longitude());
            assertEquals(1000.0, result.radius());
        }
    }

    @Nested
    @DisplayName("findNearby - 搜尋參數驗證")
    class ValidationTests {

        @Test
        @DisplayName("未提供座標卻指定半徑應回傳 400")
        void radiusWithoutCoordinates() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMapService.findNearby(
                            null, null, null, null, 5000.0));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals(
                    "未提供座標時不可指定搜尋半徑",
                    exception.getReason());

            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("只提供緯度應回傳 400")
        void missingLongitude() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMapService.findNearby(
                            null, null, 25.0, null, null));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals(
                    "緯度與經度必須同時提供",
                    exception.getReason());

            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("只提供經度應回傳 400")
        void missingLatitude() {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> cinemaMapService.findNearby(
                            null, null, null, 121.0, null));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals(
                    "緯度與經度必須同時提供",
                    exception.getReason());

            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("緯度超出 -90 至 90 應回傳 400")
        void invalidLatitude() {
            double[] invalidValues = {
                    -90.1, 90.1,
                    Double.NaN,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY
            };

            for (double latitude : invalidValues) {
                ResponseStatusException exception = assertThrows(
                        ResponseStatusException.class,
                        () -> cinemaMapService.findNearby(
                                null, null, latitude, 121.0, null));

                assertEquals(
                        HttpStatus.BAD_REQUEST,
                        exception.getStatusCode());

                assertEquals(
                        "座標超出合法範圍",
                        exception.getReason());
            }

            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("經度超出 -180 至 180 應回傳 400")
        void invalidLongitude() {
            double[] invalidValues = {
                    -180.1, 180.1,
                    Double.NaN,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY
            };

            for (double longitude : invalidValues) {
                ResponseStatusException exception = assertThrows(
                        ResponseStatusException.class,
                        () -> cinemaMapService.findNearby(
                                null, null, 25.0, longitude, null));

                assertEquals(
                        HttpStatus.BAD_REQUEST,
                        exception.getStatusCode());

                assertEquals(
                        "座標超出合法範圍",
                        exception.getReason());
            }

            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("搜尋半徑為零、負數或非有限數值應回傳 400")
        void invalidRadius() {
            double[] invalidValues = {
                    0.0, -1.0,
                    Double.NaN,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY
            };

            for (double radius : invalidValues) {
                ResponseStatusException exception = assertThrows(
                        ResponseStatusException.class,
                        () -> cinemaMapService.findNearby(
                                null, null, 25.0, 121.0, radius));

                assertEquals(
                        HttpStatus.BAD_REQUEST,
                        exception.getStatusCode());

                assertEquals(
                        "搜尋半徑必須為正數",
                        exception.getReason());
            }

            verifyNoInteractions(cinemaRepository);
        }

        @Test
        @DisplayName("緯度與經度邊界值應視為合法座標")
        void boundaryCoordinatesShouldBeValid() {
            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of());

            double[][] coordinates = {
                    { -90.0, -180.0 },
                    { 90.0, 180.0 },
                    { 0.0, 0.0 }
            };

            for (double[] coordinate : coordinates) {
                CinemaMapResponse result = cinemaMapService.findNearby(
                        null, null,
                        coordinate[0], coordinate[1],
                        1000.0);

                assertNotNull(result);
                assertEquals(0, result.total());
                assertEquals(coordinate[0], result.latitude());
                assertEquals(coordinate[1], result.longitude());
            }
        }

        @Test
        @DisplayName("極小的正數半徑應視為合法")
        void smallPositiveRadiusShouldBeValid() {
            CinemaBean center = cinema(1, "中心影城", 25.0, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(center));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 0.001);

            assertNotNull(result);
            assertEquals(1, result.total());
            assertEquals(0.001, result.radius());
            assertEquals(0.0, result.cinemas().get(0).distance());
        }
    }
}
