package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.MessageBean;

public interface MessageRepository
        extends JpaRepository<MessageBean, Integer> {

    // 分頁查詢聊天室訊息，最新的訊息優先
    Page<MessageBean>
        findByConversation_ConversationIdOrderBySentAtDesc(
            Integer conversationId,
            Pageable pageable
        );

    // 計算聊天室內尚未讀取的訊息
    long countByConversation_ConversationIdAndIsReadFalse(
        Integer conversationId
    );

    // 查詢聊天室內尚未讀取的訊息，排除目前登入會員自己發送的訊息
    List<MessageBean> findByConversation_ConversationIdAndSender_UsersIdNotAndIsReadFalse(
            Integer conversationId, Integer usersId);

    Optional<MessageBean> findTopByConversation_ConversationIdOrderBySentAtDescMessageIdDesc(
        Integer conversationId);

    long countByConversation_ConversationIdAndSender_UsersIdNotAndIsReadFalse(
            Integer conversationId, Integer usersId);

}