package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.collection.benefitsjourney.domain.InventoryReportBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.UserContributionBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.InventoryReportCreateRequest;
import com.collection.benefitsjourney.dto.InventoryReportResponse;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.InventoryReportRepository;
import com.collection.benefitsjourney.repository.UserContributionRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class InventoryReportServiceTest {

    @Mock private InventoryReportRepository inventoryReportRepository;
    @Mock private CinemaMovieBenefitRepository cinemaMovieBenefitRepository;
    @Mock private UsersRepository usersRepository;
    @Mock private UserContributionRepository userContributionRepository;
    @InjectMocks private InventoryReportService service;

    private UsersBean user;
    private CinemaMovieBenefitBean link;

    @BeforeEach
    void setUp() {
        user = new UsersBean();
        user.setUsersId(1);
        user.setStatus("ACTIVE");

        CinemaBean cinema = new CinemaBean();
        cinema.setCinemaId(100);
        cinema.setCinemaName("信義威秀");
        MovieBean movie = new MovieBean();
        movie.setMovieId(200);
        movie.setMovieName("測試電影");
        BenefitBean benefit = new BenefitBean();
        benefit.setBenefitId(300);
        benefit.setBenefitName("測試特典");
        benefit.setStatus("APPROVED");

        link = new CinemaMovieBenefitBean();
        link.setCinemaMovieBenefitId(10);
        link.setCinema(cinema);
        link.setMovie(movie);
        link.setBenefit(benefit);
    }

    private InventoryReportCreateRequest request(BenefitInventoryStatus status) {
        return new InventoryReportCreateRequest(10, status);
    }

    @Test
    @DisplayName("非ACTIVE帳號回403")
    void inactiveUser() {
        user.setStatus("BANNED");
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1, request(BenefitInventoryStatus.IN_STOCK)));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }

    @Test
    @DisplayName("回報UNKNOWN回400")
    void unknownStatus() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1, request(BenefitInventoryStatus.UNKNOWN)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    @DisplayName("找不到中介表ID回404")
    void linkNotFound() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(cinemaMovieBenefitRepository.findById(10)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1, request(BenefitInventoryStatus.IN_STOCK)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    @DisplayName("特典已下架回409")
    void expiredBenefit() {
        link.getBenefit().setStatus("EXPIRED");
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(cinemaMovieBenefitRepository.findById(10)).thenReturn(Optional.of(link));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1, request(BenefitInventoryStatus.IN_STOCK)));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(inventoryReportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("15分鐘內重複回報回429，且不寫入任何資料")
    void cooldown() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(cinemaMovieBenefitRepository.findById(10)).thenReturn(Optional.of(link));
        when(inventoryReportRepository
                .existsByUser_UsersIdAndCinemaMovieBenefit_CinemaMovieBenefitIdAndReportedAtAfter(
                        eq(1), eq(10), any(LocalDateTime.class)))
                .thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(1, request(BenefitInventoryStatus.IN_STOCK)));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
        verify(inventoryReportRepository, never()).saveAndFlush(any());
        verify(userContributionRepository, never()).save(any());
    }

    @Test
    @DisplayName("成功：寫入回報、更新中介表狀態、新增貢獻")
    void success() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(cinemaMovieBenefitRepository.findById(10)).thenReturn(Optional.of(link));
        when(inventoryReportRepository
                .existsByUser_UsersIdAndCinemaMovieBenefit_CinemaMovieBenefitIdAndReportedAtAfter(
                        eq(1), eq(10), any(LocalDateTime.class)))
                .thenReturn(false);
        when(inventoryReportRepository.saveAndFlush(any(InventoryReportBean.class)))
                .thenAnswer(inv -> {
                    InventoryReportBean r = inv.getArgument(0);
                    r.setReportId(5);
                    r.setReportedAt(LocalDateTime.of(2026, 10, 14, 12, 0));
                    return r;
                });
        when(inventoryReportRepository
                .findFirstByCinemaMovieBenefit_CinemaMovieBenefitIdOrderByReportedAtDescReportIdDesc(10))
                .thenAnswer(inv -> {
                    InventoryReportBean r = new InventoryReportBean();
                    r.setReportId(5);
                    r.setStatus(BenefitInventoryStatus.LOW_STOCK.getCode());
                    return Optional.of(r);
                });

        InventoryReportResponse result = service.create(1, request(BenefitInventoryStatus.LOW_STOCK));

        assertEquals(5, result.reportId());
        assertEquals("LOW_STOCK", result.status());
        assertEquals("信義威秀", result.cinemaName());
        assertEquals(BenefitInventoryStatus.LOW_STOCK.getCode(), link.getBenefitInventoryStatus());
        verify(cinemaMovieBenefitRepository).save(link);

        ArgumentCaptor<UserContributionBean> captor = ArgumentCaptor.forClass(UserContributionBean.class);
        verify(userContributionRepository).save(captor.capture());
        assertEquals("INVENTORY_REPORT", captor.getValue().getContributionType());
        assertEquals(5, captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("中介表狀態以「回報時間最新」的那筆為準，不是這次送出的值")
    void latestReportWins() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(cinemaMovieBenefitRepository.findById(10)).thenReturn(Optional.of(link));
        when(inventoryReportRepository
                .existsByUser_UsersIdAndCinemaMovieBenefit_CinemaMovieBenefitIdAndReportedAtAfter(
                        eq(1), eq(10), any(LocalDateTime.class)))
                .thenReturn(false);
        when(inventoryReportRepository.saveAndFlush(any(InventoryReportBean.class)))
                .thenAnswer(inv -> {
                    InventoryReportBean r = inv.getArgument(0);
                    r.setReportId(5);
                    return r;
                });
        // 另一位使用者稍晚才回報「已售罄」，但先 commit
        when(inventoryReportRepository
                .findFirstByCinemaMovieBenefit_CinemaMovieBenefitIdOrderByReportedAtDescReportIdDesc(10))
                .thenAnswer(inv -> {
                    InventoryReportBean r = new InventoryReportBean();
                    r.setReportId(6);
                    r.setStatus(BenefitInventoryStatus.EXHAUSTED.getCode());
                    return Optional.of(r);
                });

        service.create(1, request(BenefitInventoryStatus.IN_STOCK));

        assertEquals(BenefitInventoryStatus.EXHAUSTED.getCode(), link.getBenefitInventoryStatus());
    }
}

