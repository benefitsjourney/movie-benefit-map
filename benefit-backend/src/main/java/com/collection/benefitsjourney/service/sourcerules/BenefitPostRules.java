package com.collection.benefitsjourney.service.sourcerules;

import java.util.List;
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
    default List<String> defaultCinemasByFormat(String normalizedBody) {
        return List.of();
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

    /** 品牌名稱，寫進回報單的影城欄位（例如「威秀影城」），沒有就回傳null */
    default String brandName() {
        return null;
    }

    /**
     * 一篇貼文是否要依「購買…即送／即可獲得」逐筆拆開（每一樣特典、每一個版本、每一個期間各一筆）。
     * 預設不拆，照原本一篇一筆的方式解析。
     */
    default boolean splitOffers() {
        return false;
    }

    /**
     * 版本限定的特典：依購買條件（例如「限SCREENX版本」「IMAX/4DX版本」）回傳有該廳的影城簡稱。
     * 不是版本限定（不限版本、任一版本、一般版本、2D）回傳null，代表全連鎖影城。
     * @param condition 購買條件那一段文字
     * @param body 整篇正文，用來找「特殊版本為…」這種定義
     */
    default List<String> versionCinemas(String condition, String body) {
        return null;
    }

    /**
     * 從一段文字找出影城簡稱。回傳null代表用Parser預設的切法（依、｜/切開）。
     * 品牌有固定影城清單時可以覆寫，只回傳認得的影城，例如「📍大巨蛋 10/3（六）」→ 大巨蛋。
     */
    default List<String> extractCinemaNames(String text) {
        return null;
    }
}


