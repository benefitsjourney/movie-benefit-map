package com.collection.benefitsjourney.service.CinemaMovie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
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

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.dto.CinemaMovie.CinemaMapResponse;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;

@ExtendWith(MockitoExtension.class)
class CinemaMapServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private CinemaMovieBenefitRepository cinemaMovieBenefitRepository;

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

    private CinemaMovieBenefitBean benefit(
            Integer id,
            CinemaBean cinema,
            Integer movieId,
            String movieName,
            Integer benefitId,
            String benefitName) {

        MovieBean movie = new MovieBean();
        movie.setMovieId(movieId);
        movie.setMovieName(movieName);

        BenefitBean benefit = new BenefitBean();
        benefit.setBenefitId(benefitId);
        benefit.setBenefitName(benefitName);

        CinemaMovieBenefitBean link = new CinemaMovieBenefitBean();
        link.setCinemaMovieBenefitId(id);
        link.setCinema(cinema);
        link.setMovie(movie);
        link.setBenefit(benefit);
        link.setBenefitInventoryStatus(BenefitInventoryStatus.IN_STOCK.getCode());

        return link;
    }

    @Nested
    @DisplayName("findNearby - 附近搜尋")
    class NearbyTests {

        @Test
        @DisplayName("關鍵字與縣市篩選條件應正確傳入 Repository")
        void shouldPassFilters() {
            when(cinemaRepository.findAllForMap(
                    any(String.class),
                    eq(County.TAIPEI)))
                    .thenReturn(List.of());

            CinemaMapResponse result = cinemaMapService.findNearby(
                    "威秀", County.TAIPEI, 25.0, 121.0, 1000.0,
                    null, null, null);

            assertNotNull(result);
            assertEquals(0, result.total());
            assertTrue(result.cinemas().isEmpty());
            assertEquals(25.0, result.latitude());
            assertEquals(121.0, result.longitude());
            assertEquals(1000.0, result.radius());

            ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);

            verify(cinemaRepository).findAllForMap(
                    keywordCaptor.capture(),
                    eq(County.TAIPEI));

            // 不綁死 LikeKeywords 內部的跳脫規則
            assertTrue(keywordCaptor.getValue().contains("威秀"));

            // 沒有半徑內影城時不查特典
            verify(cinemaMovieBenefitRepository, never()).findApprovedByCinemaIds(any(), any(), any());
        }

        @Test
        @DisplayName("沒有符合條件的影城時應回傳空清單")
        void shouldReturnEmptyList() {
            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of());

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0,
                    null, null, null);

            assertNotNull(result);
            assertEquals(0, result.total());
            assertTrue(result.cinemas().isEmpty());
            assertEquals(25.0, result.latitude());
            assertEquals(121.0, result.longitude());
            assertEquals(5000.0, result.radius());

            verify(cinemaMovieBenefitRepository, never()).findApprovedByCinemaIds(any(), any(), any());
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
                    null, null, 25.0, 121.0, 1000.0,
                    null, null, null);

            assertNotNull(result);
            assertEquals(2, result.total());
            assertEquals(1000.0, result.radius());

            assertEquals(1, result.cinemas().get(0).cinemaId());
            assertEquals(2, result.cinemas().get(1).cinemaId());

            assertTrue(result.cinemas().stream()
                    .allMatch(cinema -> cinema.distance() <= 1000.0));

            // 特典查詢只帶入半徑內、且已依距離排序的影城 ID
            verify(cinemaMovieBenefitRepository).findApprovedByCinemaIds(
                    eq(List.of(1, 2)),
                    isNull(),
                    isNull());
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
                    null, null, 25.0, 121.0, 1000.0,
                    null, null, null);

            assertEquals(1, result.total());
            assertEquals(0.0, result.cinemas().get(0).distance());
            assertEquals(0, result.cinemas().get(0).benefitCount());
            assertTrue(result.cinemas().get(0).benefits().isEmpty());
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
                    null, null, 25.0, 121.0, 5000.0,
                    null, null, null);

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
                    null, null, 25.0, 121.0, 5000.0,
                    null, null, null);

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
                    null, null, 25.0, 121.0, 5000.0,
                    null, null, null);

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
                    null, null, 0.0, 0.0, 100.0,
                    null, null, null);

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
                    null, null, 25.0, 121.0, 1000.0,
                    null, null, null);

            assertNotNull(result);
            assertEquals(0, result.total());
            assertTrue(result.cinemas().isEmpty());

            // 即使結果為空，仍應保留搜尋參數
            assertEquals(25.0, result.latitude());
            assertEquals(121.0, result.longitude());
            assertEquals(1000.0, result.radius());

            verify(cinemaMovieBenefitRepository, never()).findApprovedByCinemaIds(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("findNearby - 特典條件")
    class BenefitTests {

        @Test
        @DisplayName("上架特典應依影城分組，並保留查詢結果順序")
        void shouldGroupBenefitsByCinema() {
            CinemaBean first = cinema(1, "影城A", 25.0, 121.0);
            CinemaBean second = cinema(2, "影城B", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(second, first));
            when(cinemaMovieBenefitRepository.findApprovedByCinemaIds(
                    eq(List.of(1, 2)),
                    isNull(),
                    isNull()))
                    .thenReturn(List.of(
                            benefit(11, first, 101, "電影甲", 201, "海報"),
                            benefit(12, first, 102, "電影乙", 202, "明信片"),
                            benefit(21, second, 101, "電影甲", 201, "海報")));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0,
                    null, null, null);

            assertEquals(2, result.total());

            CinemaMapResponse.Cinema cinemaA = result.cinemas().get(0);
            assertEquals(1, cinemaA.cinemaId());
            assertEquals(2, cinemaA.benefitCount());
            assertEquals(List.of(11, 12), cinemaA.benefits().stream()
                    .map(benefit -> benefit.cinemaMovieBenefitId())
                    .toList());
            assertEquals("IN_STOCK", cinemaA.benefits().get(0).status());
            assertEquals("電影甲", cinemaA.benefits().get(0).movieName());
            assertEquals("海報", cinemaA.benefits().get(0).benefitName());

            CinemaMapResponse.Cinema cinemaB = result.cinemas().get(1);
            assertEquals(2, cinemaB.cinemaId());
            assertEquals(1, cinemaB.benefitCount());
            assertEquals(21, cinemaB.benefits().get(0).cinemaMovieBenefitId());
        }

        @Test
        @DisplayName("未指定特典條件時，沒有特典的影城仍應回傳")
        void shouldKeepCinemasWithoutBenefits() {
            CinemaBean withBenefit = cinema(1, "有特典", 25.0, 121.0);
            CinemaBean withoutBenefit = cinema(2, "無特典", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(withBenefit, withoutBenefit));
            when(cinemaMovieBenefitRepository.findApprovedByCinemaIds(any(), isNull(), isNull()))
                    .thenReturn(List.of(
                            benefit(11, withBenefit, 101, "電影甲", 201, "海報")));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0,
                    null, null, false);

            assertEquals(2, result.total());
            assertEquals(1, result.cinemas().get(0).benefitCount());
            assertEquals(0, result.cinemas().get(1).benefitCount());
        }

        @Test
        @DisplayName("hasBenefit 為 true 時只回傳有上架特典的影城")
        void shouldFilterCinemasWithoutBenefitsWhenHasBenefit() {
            CinemaBean withBenefit = cinema(1, "有特典", 25.0, 121.0);
            CinemaBean withoutBenefit = cinema(2, "無特典", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(withBenefit, withoutBenefit));
            when(cinemaMovieBenefitRepository.findApprovedByCinemaIds(
                    eq(List.of(1, 2)),
                    isNull(),
                    isNull()))
                    .thenReturn(List.of(
                            benefit(11, withBenefit, 101, "電影甲", 201, "海報")));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0,
                    null, null, true);

            assertEquals(1, result.total());
            assertEquals(1, result.cinemas().get(0).cinemaId());
            assertEquals(1, result.cinemas().get(0).benefitCount());
        }

        @Test
        @DisplayName("電影關鍵字應轉成模糊條件，且沒有符合特典的影城不回傳")
        void shouldFilterByMovieKeyword() {
            CinemaBean matched = cinema(1, "符合影城", 25.0, 121.0);
            CinemaBean unmatched = cinema(2, "未符合影城", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(matched, unmatched));
            when(cinemaMovieBenefitRepository.findApprovedByCinemaIds(
                    eq(List.of(1, 2)),
                    any(String.class),
                    isNull()))
                    .thenReturn(List.of(
                            benefit(11, matched, 101, "鬼滅之刃", 201, "海報")));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0,
                    "鬼滅", null, null);

            assertEquals(1, result.total());
            assertEquals(1, result.cinemas().get(0).cinemaId());

            ArgumentCaptor<String> movieCaptor = ArgumentCaptor.forClass(String.class);
            verify(cinemaMovieBenefitRepository).findApprovedByCinemaIds(
                    eq(List.of(1, 2)),
                    movieCaptor.capture(),
                    isNull());
            assertTrue(movieCaptor.getValue().contains("鬼滅"));
        }

        @Test
        @DisplayName("特典關鍵字應轉成模糊條件，且沒有符合特典的影城不回傳")
        void shouldFilterByBenefitKeyword() {
            CinemaBean matched = cinema(1, "符合影城", 25.0, 121.0);
            CinemaBean unmatched = cinema(2, "未符合影城", 25.01, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(unmatched, matched));
            when(cinemaMovieBenefitRepository.findApprovedByCinemaIds(
                    eq(List.of(1, 2)),
                    isNull(),
                    any(String.class)))
                    .thenReturn(List.of(
                            benefit(11, matched, 101, "電影甲", 201, "壓克力立牌")));

            CinemaMapResponse result = cinemaMapService.findNearby(
                    null, null, 25.0, 121.0, 5000.0,
                    null, "立牌", null);

            assertEquals(1, result.total());
            assertEquals(1, result.cinemas().get(0).cinemaId());
            assertEquals("壓克力立牌", result.cinemas().get(0).benefits().get(0).benefitName());

            ArgumentCaptor<String> benefitCaptor = ArgumentCaptor.forClass(String.class);
            verify(cinemaMovieBenefitRepository).findApprovedByCinemaIds(
                    eq(List.of(1, 2)),
                    isNull(),
                    benefitCaptor.capture());
            assertTrue(benefitCaptor.getValue().contains("立牌"));
        }

        @Test
        @DisplayName("空白的電影或特典關鍵字不應觸發只顯示有特典")
        void blankKeywordsShouldNotFilterCinemas() {
            CinemaBean withoutBenefit = cinema(1, "無特典", 25.0, 121.0);

            when(cinemaRepository.findAllForMap(
                    nullable(String.class),
                    nullable(County.class)))
                    .thenReturn(List.of(withoutBenefit));
            when(cinemaMovieBenefitRepository.findApprovedByCinemaIds(
                    eq(List.of(1)),
                    isNull(),
                    isNull()))
                    .thenReturn(List.of());

            CinemaMapResponse result = cinemaMapService.findNearby(
                    "  ", null, 25.0, 121.0, 5000.0,
                    " ", "　", null);

            assertEquals(1, result.total());
            assertEquals(0, result.cinemas().get(0).benefitCount());

            verify(cinemaRepository).findAllForMap(isNull(), isNull());
            verify(cinemaMovieBenefitRepository).findApprovedByCinemaIds(
                    eq(List.of(1)),
                    isNull(),
                    isNull());
        }
    }

    @Nested
    @DisplayName("findNearby - 搜尋參數驗證")
    class ValidationTests {

        @Test
        @DisplayName("緯度、經度或搜尋半徑缺一應回傳 400")
        void missingRequiredCoordinates() {
            Double[][] cases = {
                    { null, null, null },
                    { 25.0, null, null },
                    { null, 121.0, null },
                    { null, null, 5000.0 },
                    { 25.0, 121.0, null },
                    { 25.0, null, 5000.0 },
                    { null, 121.0, 5000.0 }
            };

            for (Double[] query : cases) {
                ResponseStatusException exception = assertThrows(
                        ResponseStatusException.class,
                        () -> cinemaMapService.findNearby(
                                null, null, query[0], query[1], query[2],
                                null, null, null));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                assertEquals(
                        "緯度、經度與搜尋半徑為必填",
                        exception.getReason());
            }

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
                                null, null, latitude, 121.0, 1000.0,
                                null, null, null));

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
                                null, null, 25.0, longitude, 1000.0,
                                null, null, null));

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
                                null, null, 25.0, 121.0, radius,
                                null, null, null));

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
                        1000.0,
                        null, null, null);

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
                    null, null, 25.0, 121.0, 0.001,
                    null, null, null);

            assertNotNull(result);
            assertEquals(1, result.total());
            assertEquals(0.001, result.radius());
            assertEquals(0.0, result.cinemas().get(0).distance());
        }
    }
}
