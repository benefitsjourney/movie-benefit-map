package com.collection.benefitsjourney.repository;

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
}
