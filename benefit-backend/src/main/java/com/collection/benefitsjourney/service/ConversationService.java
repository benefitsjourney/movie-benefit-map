package com.collection.benefitsjourney.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.ConversationBean;
import com.collection.benefitsjourney.domain.MessageBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.ConversationRepository;
import com.collection.benefitsjourney.repository.MessageRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@Service 
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UsersRepository usersRepository;
    private final MessageRepository messageRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            UsersRepository usersRepository,
            MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.usersRepository = usersRepository;
    this.messageRepository = messageRepository;
    }

    // 查詢目前會員參與的所有聊天室
    @Transactional(readOnly = true)
    public List<ConversationBean> getMyConversations(Integer usersId) {

        return conversationRepository
                .findByUserOne_UsersIdOrUserTwo_UsersIdOrderByUpdatedAtDesc(
                        usersId, usersId);
    }

    // 建立聊天室，若已存在則直接取得
    @Transactional
    public ConversationBean getOrCreateConversation(
            Integer currentUserId,
            Integer targetUserId) {

        // 不允許自己和自己聊天
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能與自己建立聊天室");
        }

        // 確認目前會員存在
        UsersBean currentUser = usersRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("目前會員不存在"));

        // 確認對方會員存在
        UsersBean targetUser = usersRepository.findById(targetUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("找不到此會員"));

        // 只允許與 ACTIVE 會員建立聊天室
        if (!"ACTIVE".equals(targetUser.getStatus())) {
            throw new IllegalArgumentException("此會員目前無法聊天");
        }

        // 固定較小的 ID 放在 userOne
        Integer userOneId = Math.min(currentUserId, targetUserId);
        Integer userTwoId = Math.max(currentUserId, targetUserId);

        // 先查詢是否已有聊天室
        return conversationRepository
                .findByUserOne_UsersIdAndUserTwo_UsersId(
                        userOneId, userTwoId)
                .orElseGet(() -> {

                    ConversationBean conversation = new ConversationBean();

                    if (currentUserId.equals(userOneId)) {
                        conversation.setUserOne(currentUser);
                        conversation.setUserTwo(targetUser);
                    } else {
                        conversation.setUserOne(targetUser);
                        conversation.setUserTwo(currentUser);
                    }

                    return conversationRepository.save(conversation);
                });
    }
    // 查詢聊天室最後一則訊息
    @Transactional(readOnly = true)
    public Optional<MessageBean> getLastMessage(Integer conversationId) {
        return messageRepository
                .findTopByConversation_ConversationIdOrderBySentAtDescMessageIdDesc(
                        conversationId);
    }

    // 計算目前會員尚未讀取的對方訊息數量
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer conversationId, Integer usersId) {
        return messageRepository
                .countByConversation_ConversationIdAndSender_UsersIdNotAndIsReadFalse(
                        conversationId, usersId);
    }
    
}
