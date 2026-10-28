package com.collection.benefitsjourney.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.MessageBean;
import com.collection.benefitsjourney.dto.MessageResponse;

@Service
public class MessagePushService {

    private final SimpMessagingTemplate messagingTemplate;

    public MessagePushService(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 將已儲存的訊息推送至指定聊天室
    public void pushMessage(MessageBean message) {

        MessageResponse response = new MessageResponse();

        response.setMessageId(message.getMessageId());
        response.setConversationId(
                message.getConversation().getConversationId());
        response.setSenderId(
                message.getSender().getUsersId());
        response.setSenderName(
                message.getSender().getName());
        response.setContent(message.getContent());
        response.setSentAt(message.getSentAt());
        response.setRead(message.isRead());

        Integer conversationId =
                message.getConversation().getConversationId();

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                response);
    }
}