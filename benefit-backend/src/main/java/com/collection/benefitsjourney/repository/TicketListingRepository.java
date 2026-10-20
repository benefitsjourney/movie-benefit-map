package com.collection.benefitsjourney.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.TicketListingBean;

public interface TicketListingRepository extends JpaRepository<TicketListingBean, Integer> {

    // 查詢指定狀態的票券刊登
    List<TicketListingBean> findByStatus(String status);

    // 查詢會員自己的票券刊登
    List<TicketListingBean> findByUsersBean_UsersId(Integer usersId);

    // 查詢指定 ID 且符合指定狀態的票券刊登
    Optional<TicketListingBean> findByTicketListingIdAndStatus(Integer ticketListingId, String status);

    // 查詢放映時間早於指定時間，且狀態不是指定狀態的票券
    List<TicketListingBean> findByTicketShowtimeBeforeAndStatusNot(
            LocalDateTime cutoff, String status);

    // 查詢指定狀態，且放映時間大於或等於指定時間的票券
    List<TicketListingBean> findByStatusAndTicketShowtimeGreaterThanEqual(String status, LocalDateTime cutoff);

    // 查詢指定 ID、狀態及放映時間符合公開條件的票券
    Optional<TicketListingBean> findByTicketListingIdAndStatusAndTicketShowtimeGreaterThanEqual(Integer ticketListingId,
            String status, LocalDateTime cutoff);
}
