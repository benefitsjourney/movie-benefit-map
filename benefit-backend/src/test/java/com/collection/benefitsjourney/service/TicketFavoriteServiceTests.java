package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collection.benefitsjourney.domain.TicketFavoriteBean;
import com.collection.benefitsjourney.domain.TicketListingBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.TicketFavoriteRepository;
import com.collection.benefitsjourney.repository.TicketListingRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class TicketFavoriteServiceTests {

        @Mock
        private TicketFavoriteRepository ticketFavoriteRepository;

        @Mock
        private TicketListingRepository ticketListingRepository;

        @Mock
        private UsersRepository usersRepository;

        @InjectMocks
        private TicketFavoriteService service;

        private UsersBean user;
        private TicketListingBean listing;

        @BeforeEach
        void setUp() {
                user = new UsersBean();
                user.setUsersId(1);

                // 票券刊登屬於另一位會員
                UsersBean listingOwner = new UsersBean();
                listingOwner.setUsersId(2);

                listing = new TicketListingBean();
                listing.setTicketListingId(100);
                listing.setStatus("ACTIVE");
                listing.setUsersBean(listingOwner);
        }

        // 新增成功
        @Test
        void addFavoriteSuccess() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(ticketFavoriteRepository
                                .existsByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100))
                                .thenReturn(false);

                when(ticketFavoriteRepository.save(any(TicketFavoriteBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TicketFavoriteBean result = service.addFavorite(1, 100);

                assertNotNull(result);
                assertSame(user, result.getUsersBean());
                assertSame(listing, result.getTicketListingBean());

                verify(ticketFavoriteRepository).save(any(TicketFavoriteBean.class));
        }

        // 可以收藏其他會員的 ACTIVE 票券刊登
        @Test
        void canFavoriteAnotherUsersActiveListing() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(ticketFavoriteRepository
                                .existsByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100))
                                .thenReturn(false);

                when(ticketFavoriteRepository.save(any(TicketFavoriteBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TicketFavoriteBean result = service.addFavorite(1, 100);

                assertNotNull(result);
                assertSame(user, result.getUsersBean());
                assertSame(listing, result.getTicketListingBean());

                // 確認被收藏的票券刊登確實屬於會員 2
                assertEquals(
                                2,
                                result.getTicketListingBean().getUsersBean().getUsersId());

                verify(ticketFavoriteRepository).save(any(TicketFavoriteBean.class));
        }

        // 不能收藏非 ACTIVE 票券
        @Test
        void cannotFavoriteInactiveTicketListing() {
                listing.setStatus("CLOSED");

                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(ticketFavoriteRepository, never())
                                .save(any(TicketFavoriteBean.class));
        }

        // 不能重複收藏票券
        @Test
        void cannotAddDuplicateTicketFavorite() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(ticketFavoriteRepository
                                .existsByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100))
                                .thenReturn(true);

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(ticketFavoriteRepository, never())
                                .save(any(TicketFavoriteBean.class));
        }

        // 取消收藏成功：設定並驗證該筆收藏確實屬於會員 1
        @Test
        void removeFavoriteSuccess() {
                TicketFavoriteBean favorite = new TicketFavoriteBean();
                favorite.setUsersBean(user);
                favorite.setTicketListingBean(listing);

                when(ticketFavoriteRepository
                                .findByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100))
                                .thenReturn(Optional.of(favorite));

                service.removeFavorite(1, 100);

                assertSame(user, favorite.getUsersBean());
                assertEquals(1, favorite.getUsersBean().getUsersId());
                assertSame(listing, favorite.getTicketListingBean());
                assertEquals(100, favorite.getTicketListingBean().getTicketListingId());

                verify(ticketFavoriteRepository)
                                .findByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100);
                verify(ticketFavoriteRepository).delete(favorite);
        }

        // 不能刪除其他會員的票券收藏
        @Test
        void cannotRemoveAnotherUsersTicketFavorite() {
                UsersBean otherUser = new UsersBean();
                otherUser.setUsersId(2);

                TicketFavoriteBean anotherUsersFavorite = new TicketFavoriteBean();
                anotherUsersFavorite.setUsersBean(otherUser);
                anotherUsersFavorite.setTicketListingBean(listing);

                // 會員 1 查詢自己的收藏時，找不到這筆屬於會員 2 的收藏
                when(ticketFavoriteRepository
                                .findByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100))
                                .thenReturn(Optional.empty());

                assertEquals(2, anotherUsersFavorite.getUsersBean().getUsersId());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.removeFavorite(1, 100));

                verify(ticketFavoriteRepository)
                                .findByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100);
                verify(ticketFavoriteRepository, never())
                                .delete(any(TicketFavoriteBean.class));
        }

        // 會員不存在
        @Test
        void cannotFavoriteTicketWhenUserNotFound() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(ticketListingRepository, never()).findById(anyInt());
                verify(ticketFavoriteRepository, never())
                                .save(any(TicketFavoriteBean.class));
        }

        // 票券刊登不存在
        @Test
        void cannotFavoriteWhenTicketListingNotFound() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(ticketListingRepository.findById(100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(ticketFavoriteRepository, never())
                                .save(any(TicketFavoriteBean.class));
        }

        // 找不到自己的收藏紀錄
        @Test
        void cannotRemoveTicketFavoriteWhenRecordNotFound() {
                when(ticketFavoriteRepository
                                .findByUsersBean_UsersIdAndTicketListingBean_TicketListingId(1, 100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.removeFavorite(1, 100));

                verify(ticketFavoriteRepository, never())
                                .delete(any(TicketFavoriteBean.class));
        }

        // 查詢自己的票券收藏
        @Test
        void findMyTicketFavorites() {
                List<TicketFavoriteBean> favorites = List.of(
                                new TicketFavoriteBean(),
                                new TicketFavoriteBean());

                when(ticketFavoriteRepository.findByUsersBean_UsersId(1))
                                .thenReturn(favorites);

                List<TicketFavoriteBean> result = service.findMyFavorites(1);

                assertEquals(2, result.size());

                verify(ticketFavoriteRepository).findByUsersBean_UsersId(1);
        }

        // 沒有收藏時，查詢自己的票券收藏應回傳空清單
        @Test
        void findMyTicketFavoritesWhenEmpty() {
                when(ticketFavoriteRepository.findByUsersBean_UsersId(1))
                                .thenReturn(List.of());

                List<TicketFavoriteBean> result = service.findMyFavorites(1);

                assertNotNull(result);
                assertTrue(result.isEmpty());

                verify(ticketFavoriteRepository).findByUsersBean_UsersId(1);
        }
}