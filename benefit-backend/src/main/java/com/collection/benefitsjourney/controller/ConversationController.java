package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.ConversationBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.domain.MessageBean;
import com.collection.benefitsjourney.dto.ConversationResponse;
import com.collection.benefitsjourney.service.ConversationService;


import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    // 查看目前登入會員的聊天室列表
    @GetMapping
    public List<ConversationResponse> getMyConversations(
            HttpServletRequest request) {

        Integer usersId = (Integer) request.getAttribute("usersId");

        List<ConversationBean> conversations =
                conversationService.getMyConversations(usersId);

        return conversations.stream()
                .map(conversation -> toResponse(conversation, usersId))
                .toList();
    }

    // 建立或取得與指定會員的聊天室
    @PostMapping("/{targetUserId}")
    public ConversationResponse getOrCreateConversation(
            @PathVariable Integer targetUserId,
            HttpServletRequest request) {

        Integer usersId = (Integer) request.getAttribute("usersId");

        ConversationBean conversation =
                conversationService.getOrCreateConversation(
                        usersId, targetUserId);

        return toResponse(conversation, usersId);
    }

    // 將聊天室資料轉換成回傳前端的 DTO
    private ConversationResponse toResponse(
            ConversationBean conversation,
            Integer currentUserId) {

        UsersBean otherUser;

        if (conversation.getUserOne().getUsersId().equals(currentUserId)) {
            otherUser = conversation.getUserTwo();
        } else {
            otherUser = conversation.getUserOne();
        }

        ConversationResponse response = new ConversationResponse();

        response.setConversationId(conversation.getConversationId());
        response.setOtherUserId(otherUser.getUsersId());
        response.setOtherUserName(otherUser.getName());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        // 填入最後一則訊息
        MessageBean lastMessage = conversationService
                .getLastMessage(conversation.getConversationId())
                .orElse(null);

        if (lastMessage != null) {
            response.setLastMessageContent(lastMessage.getContent());
            response.setLastMessageAt(lastMessage.getSentAt());
        }

        // 填入目前登入會員的未讀數量
        response.setUnreadCount(
                conversationService.getUnreadCount(
                        conversation.getConversationId(), currentUserId));

        return response;
    }
}

