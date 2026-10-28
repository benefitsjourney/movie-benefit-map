package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.ConversationBean;
import com.collection.benefitsjourney.domain.MessageBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.ConversationRepository;
import com.collection.benefitsjourney.repository.MessageRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UsersRepository usersRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            UsersRepository usersRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.usersRepository = usersRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // 查詢聊天室訊息
    @Transactional(readOnly = true)
    public Page<MessageBean> getMessages(
            Integer usersId, Integer conversationId, Pageable pageable) {

        checkConversationMember(usersId, conversationId);

        return messageRepository
                .findByConversation_ConversationIdOrderBySentAtDesc(
                        conversationId, pageable);
    }

    // 傳送訊息
    @Transactional
    public MessageBean sendMessage(
            Integer usersId, Integer conversationId, String content) {

        ConversationBean conversation =
                checkConversationMember(usersId, conversationId);

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("訊息內容不可為空");
        }

        content = content.trim();

        if (content.length() > 2000) {
            throw new IllegalArgumentException("訊息長度不可超過 2000 字");
        }

        UsersBean sender = usersRepository.findById(usersId)
                .orElseThrow(() -> new IllegalArgumentException("目前會員不存在"));

        MessageBean message = new MessageBean();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);

        MessageBean savedMessage = messageRepository.save(message);

        // 更新聊天室最後活動時間
        conversation.setUpdatedAt(LocalDateTime.now());
        
        conversationRepository.save(conversation);
        // 即時推送訊息給訂閱此聊天室的前端
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                savedMessage
        );

        return savedMessage;
    }

    // 將對方傳來的未讀訊息標記為已讀
    @Transactional
    public int markMessagesAsRead(
            Integer usersId, Integer conversationId) {

        checkConversationMember(usersId, conversationId);

        List<MessageBean> unreadMessages = messageRepository
                .findByConversation_ConversationIdAndSender_UsersIdNotAndIsReadFalse(
                        conversationId, usersId);

        for (MessageBean message : unreadMessages) {
            message.setRead(true);
        }

        messageRepository.saveAll(unreadMessages);

        return unreadMessages.size();
    }

    // 確認登入會員屬於這個聊天室
    private ConversationBean checkConversationMember(
            Integer usersId, Integer conversationId) {

        ConversationBean conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("聊天室不存在"));

        Integer userOneId = conversation.getUserOne().getUsersId();
        Integer userTwoId = conversation.getUserTwo().getUsersId();

        if (!userOneId.equals(usersId) && !userTwoId.equals(usersId)) {
            throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN,
            "你沒有權限存取此聊天室");
        }

        return conversation;
    }
}
