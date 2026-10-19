package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;

/** 一篇貼文解析出的一筆候選特典 */
public record ParsedBenefitPost(
        String postId,
        String sourceUrl,
        String username,
        String text,
        LocalDateTime publishedAt,
        String movieName,       // 取自貼文《電影名》，只是提示，審核時仍要比對Movie表
        String benefitName,
        Boolean memberOnly,
        String benefitType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<String> imageUrls,
        String reviewNote,
        Classification classification,
        String chainKeyword,        // 不為空＝全連鎖影城都能領（例如「威秀」），週特典用
        List<String> cinemaNames) { // 貼文列出的指定影城名稱（特別場用），可能為空

    public enum Classification { BENEFIT, NOT_BENEFIT, INCOMPLETE }
}