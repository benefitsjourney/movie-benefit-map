package com.collection.benefitsjourney.repository;

// import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collection.benefitsjourney.domain.LoginLogBean;

public interface LoginLogRepository extends JpaRepository<LoginLogBean, Integer> {
    // 查詢指定會員的所有登入紀錄，最新的排在前面
    Page<LoginLogBean> findByUser_UsersIdOrderByLoginTimeDesc(
            Integer usersId,
            Pageable pageable);

    // 按會員編號、帳號或姓名搜尋登入紀錄
    Page<LoginLogBean> findByUser_UsersIdOrUser_AccountContainingIgnoreCaseOrUser_NameContainingIgnoreCaseOrderByLoginTimeDesc(
            Integer usersId,
            String account,
            String name,
            Pageable pageable);

    // 按設備資訊搜尋登入紀錄
    Page<LoginLogBean> findByUserAgentContainingIgnoreCaseOrderByLoginTimeDesc(
            String userAgent,
            Pageable pageable);

    // 按 IP 位址搜尋登入紀錄
    Page<LoginLogBean> findByIpAddressContainingOrderByLoginTimeDesc(
            String ipAddress,
            Pageable pageable);

    // 新增：會員、設備、IP 可任意組合搜尋
    @Query("""
        SELECT log
        FROM LoginLogBean log
        LEFT JOIN log.user user
        WHERE
            (
                :memberKeyword IS NULL
                OR (
                    user IS NOT NULL
                    AND (
                        STR(user.usersId) LIKE CONCAT('%', :memberKeyword, '%')
                        OR LOWER(user.account) LIKE LOWER(CONCAT('%', :memberKeyword, '%'))
                        OR LOWER(user.name) LIKE LOWER(CONCAT('%', :memberKeyword, '%'))
                    )
                )
            )
            AND (
                :deviceKeyword IS NULL
                OR LOWER(log.userAgent) LIKE LOWER(CONCAT('%', :deviceKeyword, '%'))
            )
            AND (
                :ipKeyword IS NULL
                OR log.ipAddress LIKE CONCAT('%', :ipKeyword, '%')
            )
        """)
    Page<LoginLogBean> searchLoginLogs(
            @Param("memberKeyword") String memberKeyword,
            @Param("deviceKeyword") String deviceKeyword,
            @Param("ipKeyword") String ipKeyword,
            Pageable pageable);

}