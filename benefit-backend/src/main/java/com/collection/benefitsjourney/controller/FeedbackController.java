package com.collection.benefitsjourney.controller;

import com.collection.benefitsjourney.dto.FeedbackAdminUpdateRequest;
import com.collection.benefitsjourney.dto.FeedbackCreateRequest;
import com.collection.benefitsjourney.dto.FeedbackResponse;
import com.collection.benefitsjourney.service.FeedbackService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // 會員提交意見回饋
    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> createFeedback(
            @RequestAttribute("usersId") Integer usersId,
            @Valid @RequestBody FeedbackCreateRequest request
    ) {
        FeedbackResponse response =
                feedbackService.createFeedback(usersId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 會員查詢自己的回饋
    @GetMapping("/feedback/me")
    public ResponseEntity<Page<FeedbackResponse>> getMyFeedback(
            @RequestAttribute("usersId") Integer usersId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                feedbackService.getMyFeedback(usersId, pageable)
        );
    }

    // 管理員查詢全部回饋，可選擇狀態
    @GetMapping("/admin/feedback")
    public ResponseEntity<Page<FeedbackResponse>> getAllFeedback(
            @RequestAttribute("usersId") Integer usersId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                feedbackService.getAllFeedback(
                        usersId,
                        status,
                        pageable
                )
        );
    }

    // 管理員查看單筆回饋
    @GetMapping("/admin/feedback/{id}")
    public ResponseEntity<FeedbackResponse> getFeedbackById(
            @RequestAttribute("usersId") Integer usersId,
            @PathVariable("id") Integer feedbackId
    ) {
        return ResponseEntity.ok(
                feedbackService.getFeedbackById(
                        usersId,
                        feedbackId
                )
        );
    }

    // 管理員更新狀態及回覆
    @PutMapping("/admin/feedback/{id}")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @RequestAttribute("usersId") Integer usersId,
            @PathVariable("id") Integer feedbackId,
            @Valid @RequestBody FeedbackAdminUpdateRequest request
    ) {
        return ResponseEntity.ok(
                feedbackService.updateFeedback(
                        usersId,
                        feedbackId,
                        request
                )
        );
    }

    // 分頁參數驗證及排序
    private Pageable createPageable(int page, int size) {

        if (page < 0) {
            throw new org.springframework.web.server
                    .ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "頁碼不可小於 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new org.springframework.web.server
                    .ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "每頁筆數必須介於 1 至 100"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
