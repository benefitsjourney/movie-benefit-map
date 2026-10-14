package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

public record ParsedBenefitPost(
        String postId,
        String sourceUrl,
        String username,
        String text,
        LocalDateTime publishedAt,
        String benefitName,
        Boolean memberOnly,
        String benefitType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<String> imageUrls,
        String reviewNote,
        Classification classification) {

    public enum Classification { BENEFIT, NOT_BENEFIT, INCOMPLETE }
}
