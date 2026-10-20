package com.collection.benefitsjourney.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        Optional<TicketListingBean> findByTicketListingIdAndStatusAndTicketShowtimeGreaterThanEqual(
                        Integer ticketListingId,
                        String status, LocalDateTime cutoff);

        // 公開查詢票券刊登：
        // 電影名稱、影城名稱、會員姓名、刊登標題彼此 OR。
        // 日期區間若有提供，則必須同時符合日期條件。
        // cutoff 保留原本公開查詢只顯示明天或之後票券的規則。
        // 使用 SQL Server 原生 SQL。
        @Query(value = """
                        SELECT listing.*
                        FROM ticket_listing listing
                        LEFT JOIN movie m
                        ON listing.movie_id = m.movie_id
                        LEFT JOIN cinema c
                        ON listing.cinema_id = c.cinema_id
                        LEFT JOIN users u
                        ON listing.users_id = u.users_id
                        WHERE listing.status = :status
                        AND listing.ticket_showtime >= :cutoff
                        AND (
                                (
                                :movieName IS NULL
                                AND :cinemaName IS NULL
                                AND :usersName IS NULL
                                AND :title IS NULL
                                )
                                OR (
                                :movieName IS NOT NULL
                                AND LOWER(m.movie_name)
                                        LIKE LOWER(CONCAT('%', :movieName, '%'))
                                )
                                OR (
                                :cinemaName IS NOT NULL
                                AND LOWER(c.cinema_name)
                                        LIKE LOWER(CONCAT('%', :cinemaName, '%'))
                                )
                                OR (
                                :usersName IS NOT NULL
                                AND LOWER(u.name)
                                        LIKE LOWER(CONCAT('%', :usersName, '%'))
                                )
                                OR (
                                :title IS NOT NULL
                                AND LOWER(listing.title)
                                        LIKE LOWER(CONCAT('%', :title, '%'))
                                )
                        )
                        AND (
                                :startDateTime IS NULL
                                OR listing.ticket_showtime >= :startDateTime
                        )
                        AND (
                                :endDateExclusive IS NULL
                                OR listing.ticket_showtime < :endDateExclusive
                        )
                        """, nativeQuery = true)
        List<TicketListingBean> searchActiveListings(
                        @Param("status") String status,
                        @Param("cutoff") LocalDateTime cutoff,
                        @Param("movieName") String movieName,
                        @Param("cinemaName") String cinemaName,
                        @Param("usersName") String usersName,
                        @Param("title") String title,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateExclusive") LocalDateTime endDateExclusive);
}
