package com.collection.benefitsjourney.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.domain.MessageBean;
import com.collection.benefitsjourney.dto.MessageResponse;
import com.collection.benefitsjourney.dto.SendMessageRequest;
import com.collection.benefitsjourney.service.MessageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // 查詢聊天室訊息，預設每頁 20 筆
    @GetMapping
    public Page<MessageResponse> getMessages(
            @PathVariable Integer conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("分頁參數不正確");
        }

        Integer usersId = (Integer) request.getAttribute("usersId");
        Pageable pageable = PageRequest.of(page, size);

        return messageService.getMessages(usersId, conversationId, pageable)
                .map(this::toResponse);
    }

    // 傳送訊息
    @PostMapping
    public MessageResponse sendMessage(
            @PathVariable Integer conversationId,
            @Valid @RequestBody SendMessageRequest body,
            HttpServletRequest request) {

        Integer usersId = (Integer) request.getAttribute("usersId");

        MessageBean message = messageService.sendMessage(
                usersId, conversationId, body.getContent());

        return toResponse(message);
    }

    // 將對方傳來的未讀訊息標記為已讀
    @PatchMapping("/read")
    public Map<String, Object> markMessagesAsRead(
            @PathVariable Integer conversationId,
            HttpServletRequest request) {

        Integer usersId = (Integer) request.getAttribute("usersId");

        int count = messageService.markMessagesAsRead(
                usersId, conversationId);

        return Map.of(
                "conversationId", conversationId,
                "markedReadCount", count,
                "message", "已將對方傳來的未讀訊息標記為已讀");
    }

    private MessageResponse toResponse(MessageBean message) {
        MessageResponse response = new MessageResponse();

        response.setMessageId(message.getMessageId());
        response.setConversationId(
                message.getConversation().getConversationId());
        response.setSenderId(message.getSender().getUsersId());
        response.setSenderName(message.getSender().getName());
        response.setContent(message.getContent());
        response.setSentAt(message.getSentAt());
        response.setRead(message.isRead());

        return response;
    }
}
