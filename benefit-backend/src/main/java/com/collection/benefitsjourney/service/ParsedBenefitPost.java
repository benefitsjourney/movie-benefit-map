package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

/** 一篇貼文解析出的「一筆」候選特典；一篇貼文有多個特典時，parseAll 會回傳多筆（共用貼文資料）。 */
public record ParsedBenefitPost(
        String postId,
        String sourceUrl,
        String username,
        String text,
        LocalDateTime publishedAt,
        String movieName,       // 取自貼文第一個《電影名》，只是提示，審核時仍要比對Movie表
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
