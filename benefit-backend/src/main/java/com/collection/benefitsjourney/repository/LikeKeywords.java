package com.collection.benefitsjourney.repository;

public final class LikeKeywords {

    private LikeKeywords() {
    }

    /** 避免SQL Server把參數轉成varchar */
    public static String toLikeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword + "%";
    }
}
