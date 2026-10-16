package com.collection.benefitsjourney.service.sourcerules;

public interface BenefitPostRules {

    /** 判斷這篇貼文是否屬於此影城 */
    boolean supports(String username, String sourceUrl);

    /** 是否包含該影城的特別場罐頭文 */
    default boolean hasSpecialBoilerplate(String originalText) {
        return false;
    }

    /** 是否包含該影城的一般週特典罐頭文 */
    default boolean hasRegularBoilerplate(String originalText) {
        return false;
    }

    /** 是否包含可用來判斷特典公告的來源專屬文案 */
    boolean isBenefitAnnouncement(String originalText);
}