package com.collection.benefitsjourney.service;

import com.collection.benefitsjourney.domain.Feedback;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.FeedbackAdminUpdateRequest;
import com.collection.benefitsjourney.dto.FeedbackCreateRequest;
import com.collection.benefitsjourney.dto.FeedbackResponse;
import com.collection.benefitsjourney.repository.FeedbackRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final EntityManager entityManager;

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "FEATURE_SUGGESTION",
            "BUG_REPORT",
            "QUESTION",
            "OTHER"
    );

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING",
            "PROCESSING",
            "COMPLETED"
    );

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            EntityManager entityManager
    ) {
        this.feedbackRepository = feedbackRepository;
        this.entityManager = entityManager;
    }

    // 取得有效的登入會員
    private UsersBean getActiveUser(Integer usersId) {

        if (usersId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "請先登入"
            );
        }

        UsersBean user = entityManager.find(UsersBean.class, usersId);

        if (user == null
                || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "會員帳號目前無法使用"
            );
        }

        return user;
    }

    // 確認登入者為有效管理員
    private UsersBean getAdmin(Integer usersId) {

        UsersBean user = getActiveUser(usersId);

        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "只有管理員可以執行此操作"
            );
        }

        return user;
    }

    // 會員提交回饋
    @Transactional
    public FeedbackResponse createFeedback(
            Integer usersId,
            FeedbackCreateRequest request
    ) {
        UsersBean user = getActiveUser(usersId);

        String category = request.getCategory().trim();
        String title = request.getTitle().trim();
        String content = request.getContent().trim();

        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效的回饋類型"
            );
        }

        if (title.isEmpty() || content.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "標題與回饋內容不可只有空白"
            );
        }

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setCategory(category);
        feedback.setTitle(title);
        feedback.setContent(content);
        feedback.setStatus("PENDING");

        Feedback saved = feedbackRepository.save(feedback);

        return FeedbackResponse.fromEntity(saved);
    }

    // 會員查詢自己的回饋
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getMyFeedback(
            Integer usersId,
            Pageable pageable
    ) {
        getActiveUser(usersId);

        return feedbackRepository
                .findByUser_UsersId(usersId, pageable)
                .map(FeedbackResponse::fromEntity);
    }


    // 管理員查詢全部回饋，可依狀態篩選
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedback(
            Integer usersId,
            String status,
            Pageable pageable
    ) {
        getAdmin(usersId);

        Page<Feedback> feedbackPage;

        if (status == null || status.isBlank()) {
            feedbackPage = feedbackRepository.findAll(pageable);
        } else {
            String normalizedStatus = status.trim().toUpperCase();

            if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "無效的回饋處理狀態"
                );
            }

            feedbackPage = feedbackRepository.findByStatus(
                    normalizedStatus,
                    pageable
            );
        }

        return feedbackPage.map(FeedbackResponse::fromEntity);
    }

    // 管理員查詢單筆回饋
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(
            Integer usersId,
            Integer feedbackId
    ) {
        getAdmin(usersId);

        Feedback feedback = feedbackRepository
                .findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到這筆意見回饋"
                ));

        return FeedbackResponse.fromEntity(feedback);
    }

    // 管理員更新處理狀態及回覆
    @Transactional
    public FeedbackResponse updateFeedback(
            Integer usersId,
            Integer feedbackId,
            FeedbackAdminUpdateRequest request
    ) {
        getAdmin(usersId);

        String status = request.getStatus().trim().toUpperCase();

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效的回饋處理狀態"
            );
        }

        Feedback feedback = feedbackRepository
                .findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到這筆意見回饋"
                ));

        String reply = request.getAdminReply();

        if (reply != null && reply.isBlank()) {
            reply = null;
        } else if (reply != null) {
            reply = reply.trim();
        }

        feedback.setStatus(status);
        feedback.setAdminReply(reply);

        Feedback updated = feedbackRepository.save(feedback);

        return FeedbackResponse.fromEntity(updated);
    }
}
