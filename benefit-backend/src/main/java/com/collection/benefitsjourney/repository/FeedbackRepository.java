
package com.collection.benefitsjourney.repository;

import com.collection.benefitsjourney.domain.Feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository
        extends JpaRepository<Feedback, Integer> {

    // 查詢指定會員的回饋
    Page<Feedback> findByUser_UsersId(
            Integer usersId,
            Pageable pageable
    );

    // 管理員依處理狀態查詢回饋
    Page<Feedback> findByStatus(
            String status,
            Pageable pageable
    );
}
