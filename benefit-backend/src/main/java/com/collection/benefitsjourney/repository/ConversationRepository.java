package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.ConversationBean;

public interface ConversationRepository
        extends JpaRepository<ConversationBean, Integer> {

    // 查詢兩位會員之間是否已有聊天室
    Optional<ConversationBean>
        findByUserOne_UsersIdAndUserTwo_UsersId(
            Integer userOneId,
            Integer userTwoId
        );

    // 查詢會員參與的所有聊天室
    List<ConversationBean>
        findByUserOne_UsersIdOrUserTwo_UsersIdOrderByUpdatedAtDesc(
            Integer userOneId,
            Integer userTwoId
        );
}