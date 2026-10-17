package com.collection.benefitsjourney.service.sourcerules;

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
}
