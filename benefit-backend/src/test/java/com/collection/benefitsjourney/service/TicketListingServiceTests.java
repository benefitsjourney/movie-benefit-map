package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.TicketListingBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.repository.TicketListingRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class TicketListingServiceTests {

        @Mock
        private TicketListingRepository ticketListingRepository;

        @Mock
        private UsersRepository usersRepository;

        @Mock
        private CinemaRepository cinemaRepository;

        @Mock
        private MovieRepository movieRepository;

        @InjectMocks
        private TicketListingService service;

        private UsersBean user;
        private CinemaBean cinema;
        private MovieBean movie;

        @BeforeEach
        void setUp() {
                user = new UsersBean();
                user.setUsersId(1);

                cinema = new CinemaBean();
                movie = new MovieBean();
        }

        // 新增成功
        @Test
        void createListingSuccess() {
                LocalDateTime showtime = LocalDateTime.of(2026, 12, 1, 19, 30);

                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(cinemaRepository.findById(10))
                                .thenReturn(Optional.of(cinema));

                when(movieRepository.findById(20))
                                .thenReturn(Optional.of(movie));

                when(ticketListingRepository.save(any(TicketListingBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TicketListingBean result = service.createListing(
                                1, "電影票轉讓", "一張票", 10, 20, showtime);

                assertNotNull(result);
                assertEquals("電影票轉讓", result.getTitle());
                assertEquals("一張票", result.getDescription());
                assertEquals("ACTIVE", result.getStatus());
                assertSame(user, result.getUsersBean());
                assertSame(cinema, result.getCinemaBean());
                assertSame(movie, result.getMovieBean());
                assertEquals(showtime, result.getTicketShowtime());

                verify(ticketListingRepository).save(any(TicketListingBean.class));
        }

        // 不能修改其他會員的票券刊登狀態
        @Test
        void cannotUpdateAnotherUsersTicketListing() {
                UsersBean otherUser = new UsersBean();
                otherUser.setUsersId(2);

                TicketListingBean listing = new TicketListingBean();
                listing.setUsersBean(otherUser);
                listing.setStatus("ACTIVE");

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "HIDDEN"));

                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 已刪除票券不能重新開啟
        @Test
        void cannotReopenDeletedTicketListing() {
                TicketListingBean listing = new TicketListingBean();
                listing.setUsersBean(user);
                listing.setStatus("DELETED");

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "ACTIVE"));

                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 已刪除票券再次設定為 DELETED：依目前 Service 規則應允許
        @Test
        void canSetDeletedTicketListingStatusToDeletedAgain() {
                TicketListingBean listing = new TicketListingBean();
                listing.setUsersBean(user);
                listing.setStatus("DELETED");

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(ticketListingRepository.save(any(TicketListingBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TicketListingBean result = service.updateStatus(1, 100, "DELETED");

                assertEquals("DELETED", result.getStatus());

                verify(ticketListingRepository).save(listing);
        }

        // 會員不存在
        @Test
        void cannotCreateTicketListingWhenUserNotFound() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.createListing(
                                                1,
                                                "電影票轉讓",
                                                "一張票",
                                                10,
                                                20,
                                                LocalDateTime.of(2026, 12, 1, 19, 30)));

                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 影城不存在
        @Test
        void cannotCreateTicketListingWhenCinemaNotFound() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(cinemaRepository.findById(10))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.createListing(
                                                1,
                                                "電影票轉讓",
                                                "一張票",
                                                10,
                                                20,
                                                LocalDateTime.of(2026, 12, 1, 19, 30)));

                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 電影不存在
        @Test
        void cannotCreateTicketListingWhenMovieNotFound() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(cinemaRepository.findById(10))
                                .thenReturn(Optional.of(cinema));

                when(movieRepository.findById(20))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.createListing(
                                                1,
                                                "電影票轉讓",
                                                "一張票",
                                                10,
                                                20,
                                                LocalDateTime.of(2026, 12, 1, 19, 30)));

                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 修改自己的票券刊登成功
        @Test
        void updateOwnTicketListingSuccess() {
                LocalDateTime showtime = LocalDateTime.of(2026, 12, 2, 20, 0);

                TicketListingBean listing = new TicketListingBean();
                listing.setUsersBean(user);
                listing.setStatus("ACTIVE");

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(cinemaRepository.findById(10))
                                .thenReturn(Optional.of(cinema));

                when(movieRepository.findById(20))
                                .thenReturn(Optional.of(movie));

                when(ticketListingRepository.save(any(TicketListingBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TicketListingBean result = service.updateListing(
                                1, 100, "新標題", "新描述", 10, 20, showtime);

                assertEquals("新標題", result.getTitle());
                assertEquals("新描述", result.getDescription());
                assertSame(cinema, result.getCinemaBean());
                assertSame(movie, result.getMovieBean());
                assertEquals(showtime, result.getTicketShowtime());

                verify(ticketListingRepository).save(listing);
        }

        // 不能修改其他會員的票券刊登內容
        @Test
        void cannotUpdateAnotherUsersTicketListingContent() {
                UsersBean otherUser = new UsersBean();
                otherUser.setUsersId(2);

                TicketListingBean listing = new TicketListingBean();
                listing.setUsersBean(otherUser);
                listing.setStatus("ACTIVE");

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateListing(
                                                1,
                                                100,
                                                "新標題",
                                                "新描述",
                                                10,
                                                20,
                                                LocalDateTime.of(2026, 12, 2, 20, 0)));

                // 權限檢查應先於影城、電影查詢
                verify(cinemaRepository, never()).findById(anyInt());
                verify(movieRepository, never()).findById(anyInt());
                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 票券刊登不存在時不能修改內容
        @Test
        void cannotUpdateTicketListingWhenNotFound() {
                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateListing(
                                                1,
                                                100,
                                                "新標題",
                                                "新描述",
                                                10,
                                                20,
                                                LocalDateTime.of(2026, 12, 2, 20, 0)));

                verify(cinemaRepository, never()).findById(anyInt());
                verify(movieRepository, never()).findById(anyInt());
                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 票券刊登不存在時不能修改狀態
        @Test
        void cannotUpdateTicketListingStatusWhenNotFound() {
                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "HIDDEN"));

                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 非法狀態
        @Test
        void cannotSetInvalidTicketListingStatus() {
                TicketListingBean listing = new TicketListingBean();
                listing.setUsersBean(user);
                listing.setStatus("ACTIVE");

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "INVALID"));

                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 已刪除刊登不能修改內容
        @Test
        void cannotUpdateDeletedTicketListing() {
                TicketListingBean listing = new TicketListingBean();
                listing.setUsersBean(user);
                listing.setStatus("DELETED");

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateListing(
                                                1,
                                                100,
                                                "新標題",
                                                "新描述",
                                                10,
                                                20,
                                                LocalDateTime.of(2026, 12, 2, 20, 0)));

                verify(cinemaRepository, never()).findById(anyInt());
                verify(ticketListingRepository, never())
                                .save(any(TicketListingBean.class));
        }

        // 公開列表只查詢 ACTIVE
        @Test
        void findActiveTicketListings() {
                List<TicketListingBean> listings = List.of(new TicketListingBean());

                when(ticketListingRepository.findByStatus("ACTIVE"))
                                .thenReturn(listings);

                List<TicketListingBean> result = service.findActiveListings();

                assertEquals(1, result.size());

                verify(ticketListingRepository).findByStatus("ACTIVE");
        }

        // 公開單筆查詢只取得 ACTIVE 票券刊登
        @Test
        void findActiveTicketListingById() {
                TicketListingBean activeListing = new TicketListingBean();
                activeListing.setStatus("ACTIVE");

                when(ticketListingRepository
                                .findByTicketListingIdAndStatus(100, "ACTIVE"))
                                .thenReturn(Optional.of(activeListing));

                TicketListingBean result = service.findActiveById(100);

                assertSame(activeListing, result);
                assertEquals("ACTIVE", result.getStatus());

                verify(ticketListingRepository)
                                .findByTicketListingIdAndStatus(100, "ACTIVE");
        }

        // 非 ACTIVE 票券刊登不能透過公開單筆查詢取得
        @Test
        void cannotFindInactiveTicketListingById() {
                when(ticketListingRepository
                                .findByTicketListingIdAndStatus(100, "ACTIVE"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.findActiveById(100));

                verify(ticketListingRepository)
                                .findByTicketListingIdAndStatus(100, "ACTIVE");
        }

        // 依會員 ID 查詢自己的票券刊登
        @Test
        void findMyTicketListings() {
                List<TicketListingBean> listings = List.of(new TicketListingBean(), new TicketListingBean());

                when(ticketListingRepository.findByUsersBean_UsersId(1))
                                .thenReturn(listings);

                List<TicketListingBean> result = service.findMyListings(1);

                assertEquals(2, result.size());

                verify(ticketListingRepository).findByUsersBean_UsersId(1);
        }
}