package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConversationResponse {
    private Integer conversationId;

    private Integer otherUserId;

    private String otherUserName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String lastMessageContent;

    private LocalDateTime lastMessageAt;
    
    private long unreadCount;
}
