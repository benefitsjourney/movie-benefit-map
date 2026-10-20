package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collection.benefitsjourney.domain.ExchangeListingBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UseBenefitRepository;

@ExtendWith(MockitoExtension.class)
class ExchangeListingServiceTests {

        @Mock
        private ExchangeListingRepository exchangeListingRepository;

        @Mock
        private UseBenefitRepository useBenefitRepository;

        @InjectMocks
        private ExchangeListingService service;

        private UsersBean user;
        private UseBenefitBean useBenefit;

        @BeforeEach
        void setUp() {
                user = new UsersBean();
                user.setUsersId(1);

                useBenefit = new UseBenefitBean();
                useBenefit.setUseBenefitId(10);
                useBenefit.setUser(user);
        }

        // 新增成功
        @Test
        void createListingSuccess() {
                when(useBenefitRepository.findById(10))
                                .thenReturn(Optional.of(useBenefit));

                when(exchangeListingRepository.findByUseBenefitBean(useBenefit))
                                .thenReturn(Optional.empty());

                when(exchangeListingRepository.save(any(ExchangeListingBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ExchangeListingBean result = service.createListing(1, "交換吊飾", "想交換其他角色", 10);

                assertNotNull(result);
                assertEquals("交換吊飾", result.getTitle());
                assertEquals("想交換其他角色", result.getDescription());
                assertEquals("ACTIVE", result.getStatus());
                assertSame(useBenefit, result.getUseBenefitBean());

                verify(exchangeListingRepository).save(any(ExchangeListingBean.class));
        }

        // 不能使用其他會員的持有紀錄
        @Test
        void cannotCreateListingUsingAnotherUsersBenefit() {
                when(useBenefitRepository.findById(10))
                                .thenReturn(Optional.of(useBenefit));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.createListing(2, "交換吊飾", "描述", 10));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 不能使用會員欄位為 null 的持有紀錄建立刊登
        @Test
        void cannotCreateListingWhenUseBenefitOwnerIsNull() {
                UseBenefitBean benefitWithoutOwner = new UseBenefitBean();
                benefitWithoutOwner.setUseBenefitId(10);
                benefitWithoutOwner.setUser(null);

                when(useBenefitRepository.findById(10))
                                .thenReturn(Optional.of(benefitWithoutOwner));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.createListing(1, "交換吊飾", "描述", 10));

                verify(exchangeListingRepository, never())
                                .findByUseBenefitBean(any(UseBenefitBean.class));
                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 不能重複刊登
        @Test
        void cannotCreateDuplicateListing() {
                ExchangeListingBean existing = new ExchangeListingBean();

                when(useBenefitRepository.findById(10))
                                .thenReturn(Optional.of(useBenefit));

                when(exchangeListingRepository.findByUseBenefitBean(useBenefit))
                                .thenReturn(Optional.of(existing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.createListing(1, "交換吊飾", "描述", 10));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 已刪除刊登不能重新開啟
        @Test
        void cannotReopenDeletedListing() {
                ExchangeListingBean listing = new ExchangeListingBean();
                listing.setStatus("DELETED");
                listing.setUseBenefitBean(useBenefit);

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "ACTIVE"));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 已刪除刊登再次設定為 DELETED：依目前 Service 規則應允許
        @Test
        void canSetDeletedListingStatusToDeletedAgain() {
                ExchangeListingBean listing = new ExchangeListingBean();
                listing.setStatus("DELETED");
                listing.setUseBenefitBean(useBenefit);

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(exchangeListingRepository.save(any(ExchangeListingBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ExchangeListingBean result = service.updateStatus(1, 100, "DELETED");

                assertEquals("DELETED", result.getStatus());

                verify(exchangeListingRepository).save(listing);
        }

        // 不能修改其他會員的刊登內容
        @Test
        void cannotUpdateAnotherUsersListing() {
                UsersBean otherUser = new UsersBean();
                otherUser.setUsersId(2);

                UseBenefitBean otherBenefit = new UseBenefitBean();
                otherBenefit.setUser(otherUser);

                ExchangeListingBean listing = new ExchangeListingBean();
                listing.setStatus("ACTIVE");
                listing.setUseBenefitBean(otherBenefit);

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateListing(1, 100, "新標題", "新描述"));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 不能修改其他會員的刊登狀態
        @Test
        void cannotUpdateAnotherUsersListingStatus() {
                UsersBean otherUser = new UsersBean();
                otherUser.setUsersId(2);

                UseBenefitBean otherBenefit = new UseBenefitBean();
                otherBenefit.setUser(otherUser);

                ExchangeListingBean listing = new ExchangeListingBean();
                listing.setStatus("ACTIVE");
                listing.setUseBenefitBean(otherBenefit);

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "HIDDEN"));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 持有紀錄不存在
        @Test
        void cannotCreateListingWhenUseBenefitNotFound() {
                when(useBenefitRepository.findById(10))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.createListing(1, "交換吊飾", "描述", 10));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 修改自己的刊登成功
        @Test
        void updateOwnListingSuccess() {
                ExchangeListingBean listing = new ExchangeListingBean();
                listing.setStatus("ACTIVE");
                listing.setUseBenefitBean(useBenefit);
                listing.setTitle("舊標題");
                listing.setDescription("舊描述");

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                when(exchangeListingRepository.save(any(ExchangeListingBean.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ExchangeListingBean result = service.updateListing(1, 100, "新標題", "新描述");

                assertEquals("新標題", result.getTitle());
                assertEquals("新描述", result.getDescription());

                verify(exchangeListingRepository).save(listing);
        }

        // 交換刊登不存在時不能修改內容
        @Test
        void cannotUpdateListingWhenNotFound() {
                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateListing(1, 100, "新標題", "新描述"));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 交換刊登不存在時不能修改狀態
        @Test
        void cannotUpdateListingStatusWhenNotFound() {
                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "HIDDEN"));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 非法狀態
        @Test
        void cannotSetInvalidListingStatus() {
                ExchangeListingBean listing = new ExchangeListingBean();
                listing.setStatus("ACTIVE");
                listing.setUseBenefitBean(useBenefit);

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateStatus(1, 100, "INVALID"));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 已刪除刊登不能修改內容
        @Test
        void cannotUpdateDeletedListing() {
                ExchangeListingBean listing = new ExchangeListingBean();
                listing.setStatus("DELETED");
                listing.setUseBenefitBean(useBenefit);

                when(exchangeListingRepository.findById(100))
                                .thenReturn(Optional.of(listing));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.updateListing(1, 100, "新標題", "新描述"));

                verify(exchangeListingRepository, never())
                                .save(any(ExchangeListingBean.class));
        }

        // 公開列表只查詢 ACTIVE
        @Test
        void findActiveExchangeListings() {
                List<ExchangeListingBean> listings = List.of(new ExchangeListingBean());

                when(exchangeListingRepository.findByStatus("ACTIVE"))
                                .thenReturn(listings);

                List<ExchangeListingBean> result = service.findActiveListings();

                assertEquals(1, result.size());

                verify(exchangeListingRepository).findByStatus("ACTIVE");
        }

        // 公開單筆查詢只取得 ACTIVE 刊登
        @Test
        void findActiveExchangeListingById() {
                ExchangeListingBean activeListing = new ExchangeListingBean();
                activeListing.setStatus("ACTIVE");

                when(exchangeListingRepository
                                .findByExchangeListingIdAndStatus(100, "ACTIVE"))
                                .thenReturn(Optional.of(activeListing));

                ExchangeListingBean result = service.findActiveById(100);

                assertSame(activeListing, result);
                assertEquals("ACTIVE", result.getStatus());

                verify(exchangeListingRepository)
                                .findByExchangeListingIdAndStatus(100, "ACTIVE");
        }

        // 非 ACTIVE 刊登不能透過公開單筆查詢取得
        @Test
        void cannotFindInactiveExchangeListingById() {
                when(exchangeListingRepository
                                .findByExchangeListingIdAndStatus(100, "ACTIVE"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.findActiveById(100));

                verify(exchangeListingRepository)
                                .findByExchangeListingIdAndStatus(100, "ACTIVE");
        }

        // 依會員 ID 查詢自己的交換刊登
        @Test
        void findMyExchangeListings() {
                List<ExchangeListingBean> listings = List.of(new ExchangeListingBean(), new ExchangeListingBean());

                when(exchangeListingRepository.findByUseBenefitBean_User_UsersId(1))
                                .thenReturn(listings);

                List<ExchangeListingBean> result = service.findMyListings(1);

                assertEquals(2, result.size());

                verify(exchangeListingRepository)
                                .findByUseBenefitBean_User_UsersId(1);
        }
}