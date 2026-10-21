package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

    @Getter
    @Setter
    @NoArgsConstructor
    public class MessageResponse {

        private Integer messageId;
        private Integer conversationId;
        private Integer senderId;
        private String senderName;
        private String content;
        private LocalDateTime sentAt;
        private boolean isRead;
    }