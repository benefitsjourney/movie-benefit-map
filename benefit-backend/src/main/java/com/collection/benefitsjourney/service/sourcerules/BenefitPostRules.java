package com.collection.benefitsjourney.service.sourcerules;

import java.util.Map;
import java.util.Optional;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;

public interface BenefitPostRules {

    /** 判斷這個來源是否屬於該影城 */
    boolean supports(String sourceUrl);

    /** 判斷貼文是否為特典公告 */
    Classification classifyPost(String originalText);

    /** 判斷特典類型，無法判斷時回傳empty */
    Optional<BenefitType> classifyBenefitType(String originalText);

    /** 移除該影城公告中的固定罐頭文，保留供Parser分析的正文 */
    default String removeBoilerplate(String originalText) {
        return originalText == null ? "" : originalText;
    }

    /** 是否包含該影城的特別場／特殊影廳線索 */
    default boolean hasSpecialScreeningClue(String originalText) {
        return false;
    }

    /** 是否包含該影城的特別場罐頭文 */
    default boolean hasSpecialBoilerplate(String originalText) {
        return false;
    }

    /** 是否包含該影城的一般週特典罐頭文。 */
    default boolean hasRegularBoilerplate(String originalText) {
        return false;
    }

    /** 一般週特典是「全連鎖影城」都能兌換時，用這個關鍵字（例如「威秀」）找出該連鎖的所有影城，沒有就回傳null */
    default String chainCinemaKeyword() {
        return null;
    }

    /** 特別場沒有列出影城、但提到特殊廳（IMAX／4DX）時，預設有該廳的影城簡稱，沒有就回傳空list */
    default java.util.List<String> defaultCinemasByFormat(String normalizedBody) {
        return java.util.List.of();
    }

    /** 「凡購買上映期間…」這種整檔期都能領、沒有起訖週期的特典（送完為止，全連鎖影城）：預設不是 */
    default boolean isWholeRunBenefit(String normalizedBody) {
        return false;
    }

    /**
     * 貼文沒有被判成特典、但有特典關鍵字時，給審核人員看的備註；回傳null代表沒有特別說明（用Parser預設的備註）。
     * 目的：讓「疑似特典」進審核，不要被悄悄丟掉。
     */
    default String incompleteNote(String originalText) {
        return null;
    }

    /** 貼文簡稱 → 資料庫影城名稱用字，例如 "MITSUI OUTLET PARK" → "三井OUTLET" */
    default Map<String, String> cinemaAliases() {
        return Map.of();
    }

    /** 預購禮型：「預購電影票即可獲得「…」」。沒有一週一週的起訖，從預售日開始、送完為止 */
    default boolean isPreorderBenefit(String normalizedBody) {
        return false;
    }
}
