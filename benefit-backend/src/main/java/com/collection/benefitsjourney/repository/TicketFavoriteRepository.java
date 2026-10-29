package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.collection.benefitsjourney.domain.TicketFavoriteBean;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketFavoriteRepository extends JpaRepository<TicketFavoriteBean, Integer> {

    // 檢查會員是否已經收藏這筆票券刊登
    boolean existsByUsersBean_UsersIdAndTicketListingBean_TicketListingId(Integer usersId, Integer ticketListingId);

    // 查詢會員對指定票券刊登的收藏
    Optional<TicketFavoriteBean> findByUsersBean_UsersIdAndTicketListingBean_TicketListingId(Integer usersId,
            Integer ticketListingId);

    // 查詢會員自己的所有票券收藏
    List<TicketFavoriteBean> findByUsersBean_UsersId(Integer usersId);

    // 刪除指定票券刊登的所有會員收藏
    @Modifying
    @Query("""
            DELETE FROM TicketFavoriteBean f
            WHERE f.ticketListingBean.ticketListingId = :ticketListingId
            """)
    int deleteAllByTicketListingId(
            @Param("ticketListingId") Integer ticketListingId);
}
