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

import com.collection.benefitsjourney.domain.ExchangeFavoriteBean;
import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.ExchangeFavoriteRepository;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class ExchangeFavoriteServiceTests {

        @Mock
        private ExchangeFavoriteRepository exchangeFavoriteRepository;

        @Mock
        private ExchangeListingRepository exchangeListingRepository;

        @Mock
        private UsersRepository usersRepository;

        @InjectMocks
        private ExchangeFavoriteService service;

        private UsersBean user;
        private ExchangeListingBean listing;

        @BeforeEach
        void setUp() {
                user = new UsersBean();
                user.setUsersId(1);

                // 交換刊登屬於另一位會員
                UsersBean listingOwner = new UsersBean();
                listingOwner.setUsersId(2);

                UseBenefitBean useBenefit = new UseBenefitBean();
                useBenefit.setUser(listingOwner);

                listing = new ExchangeListingBean();
                listing.setExchangeListingId(100);
                listing.setStatus("ACTIVE");
                listing.setUseBenefitBean(useBenefit);
        }

        // 新增成功
        @Test
        void addFavoriteSuccess() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(exchangeFavoriteRepository
                                .existsByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100))
                                .thenReturn(false);

                when(exchangeFavoriteRepository.save(any(ExchangeFavoriteBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ExchangeFavoriteBean result = service.addFavorite(1, 100);

                assertNotNull(result);
                assertSame(user, result.getUsersBean());
                assertSame(listing, result.getExchangeListingBean());

                verify(exchangeFavoriteRepository).save(any(ExchangeFavoriteBean.class));
        }

        // 可以收藏其他會員的 ACTIVE 交換刊登
        @Test
        void canFavoriteAnotherUsersActiveListing() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(exchangeFavoriteRepository
                                .existsByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100))
                                .thenReturn(false);

                when(exchangeFavoriteRepository.save(any(ExchangeFavoriteBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ExchangeFavoriteBean result = service.addFavorite(1, 100);

                assertNotNull(result);
                assertSame(user, result.getUsersBean());
                assertSame(listing, result.getExchangeListingBean());

                // 確認被收藏的刊登確實屬於會員 2
                assertEquals(
                                2,
                                result.getExchangeListingBean()
                                                .getUseBenefitBean()
                                                .getUser()
                                                .getUsersId());

                verify(exchangeFavoriteRepository).save(any(ExchangeFavoriteBean.class));
        }

        // 不能收藏非 ACTIVE 刊登
        @Test
        void cannotFavoriteInactiveExchangeListing() {
                listing.setStatus("HIDDEN");

                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(exchangeFavoriteRepository, never())
                                .save(any(ExchangeFavoriteBean.class));
        }

        // 不能重複收藏
        @Test
        void cannotAddDuplicateFavorite() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(exchangeFavoriteRepository
                                .existsByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100))
                                .thenReturn(true);

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(exchangeFavoriteRepository, never())
                                .save(any(ExchangeFavoriteBean.class));
        }

        // 取消收藏：設定並驗證該筆收藏確實屬於會員 1
        @Test
        void removeOwnFavoriteOnly() {
                ExchangeFavoriteBean favorite = new ExchangeFavoriteBean();
                favorite.setUsersBean(user);
                favorite.setExchangeListingBean(listing);

                when(exchangeFavoriteRepository
                                .findByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100))
                                .thenReturn(Optional.of(favorite));

                service.removeFavorite(1, 100);

                assertSame(user, favorite.getUsersBean());
                assertEquals(1, favorite.getUsersBean().getUsersId());
                assertSame(listing, favorite.getExchangeListingBean());
                assertEquals(100, favorite.getExchangeListingBean().getExchangeListingId());

                verify(exchangeFavoriteRepository)
                                .findByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100);
                verify(exchangeFavoriteRepository).delete(favorite);
        }

        // 不能刪除其他會員的收藏
        @Test
        void cannotRemoveAnotherUsersFavorite() {
                UsersBean otherUser = new UsersBean();
                otherUser.setUsersId(2);

                ExchangeFavoriteBean anotherUsersFavorite = new ExchangeFavoriteBean();
                anotherUsersFavorite.setUsersBean(otherUser);
                anotherUsersFavorite.setExchangeListingBean(listing);

                // 會員 1 查詢自己的收藏時，找不到這筆屬於會員 2 的收藏
                when(exchangeFavoriteRepository
                                .findByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100))
                                .thenReturn(Optional.empty());

                assertEquals(2, anotherUsersFavorite.getUsersBean().getUsersId());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.removeFavorite(1, 100));

                verify(exchangeFavoriteRepository)
                                .findByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100);
                verify(exchangeFavoriteRepository, never())
                                .delete(any(ExchangeFavoriteBean.class));
        }

        // 會員不存在
        @Test
        void cannotFavoriteWhenUserNotFound() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(exchangeListingRepository, never()).findById(anyInt());
                verify(exchangeFavoriteRepository, never())
                                .save(any(ExchangeFavoriteBean.class));
        }

        // 交換刊登不存在
        @Test
        void cannotFavoriteWhenExchangeListingNotFound() {
                when(usersRepository.findById(1))
                                .thenReturn(Optional.of(user));

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.addFavorite(1, 100));

                verify(exchangeFavoriteRepository, never())
                                .save(any(ExchangeFavoriteBean.class));
        }

        // 找不到收藏紀錄
        @Test
        void cannotRemoveFavoriteWhenRecordNotFound() {
                when(exchangeFavoriteRepository
                                .findByUsersBean_UsersIdAndExchangeListingBean_ExchangeListingId(1, 100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.removeFavorite(1, 100));

                verify(exchangeFavoriteRepository, never())
                                .delete(any(ExchangeFavoriteBean.class));
        }

        // 查詢自己的收藏
        @Test
        void findMyExchangeFavorites() {
                List<ExchangeFavoriteBean> favorites = List.of(
                                new ExchangeFavoriteBean(),
                                new ExchangeFavoriteBean());

                when(exchangeFavoriteRepository.findByUsersBean_UsersId(1))
                                .thenReturn(favorites);

                List<ExchangeFavoriteBean> result = service.findMyFavorites(1);

                assertEquals(2, result.size());

                verify(exchangeFavoriteRepository).findByUsersBean_UsersId(1);
        }

        // 沒有收藏時，查詢自己的收藏應回傳空清單
        @Test
        void findMyExchangeFavoritesWhenEmpty() {
                when(exchangeFavoriteRepository.findByUsersBean_UsersId(1))
                                .thenReturn(List.of());

                List<ExchangeFavoriteBean> result = service.findMyFavorites(1);

                assertNotNull(result);
                assertTrue(result.isEmpty());

                verify(exchangeFavoriteRepository).findByUsersBean_UsersId(1);
        }
}