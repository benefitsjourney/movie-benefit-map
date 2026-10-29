package com.collection.benefitsjourney.dto;

import com.collection.benefitsjourney.domain.Feedback;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FeedbackResponse {

    private Integer feedbackId;
    private Integer userId;
    private String account;
    private String memberName;

    private String category;
    private String title;
    private String content;

    private String status;
    private String adminReply;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FeedbackResponse fromEntity(Feedback feedback) {

        FeedbackResponse response = new FeedbackResponse();

        response.setFeedbackId(feedback.getFeedbackId());
        response.setUserId(feedback.getUser().getUsersId());
        response.setAccount(feedback.getUser().getAccount());
        response.setMemberName(feedback.getUser().getName());

        response.setCategory(feedback.getCategory());
        response.setTitle(feedback.getTitle());
        response.setContent(feedback.getContent());

        response.setStatus(feedback.getStatus());
        response.setAdminReply(feedback.getAdminReply());

        response.setCreatedAt(feedback.getCreatedAt());
        response.setUpdatedAt(feedback.getUpdatedAt());

        return response;
    }
}
