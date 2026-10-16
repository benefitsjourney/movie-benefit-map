package com.collection.benefitsjourney.service.sourcerules;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.enums.BenefitType;

@Component
public class VieshowPostRules implements BenefitPostRules {

	/** 特別場公告固定罐頭文的辨識條件。 */
	private static final Pattern SPECIAL_BOILERPLATE = Pattern.compile(
	        "愛心票／敬老票僅限「活動影城臨櫃」或「全新APP\\(限購2張\\)」購買。"
	                + "\\s*"
	                + "除以上購票通路，影城現場自動取購票機與其他通路均不適用！"
	                + "\\s*"
	                + "團券加價、銀行優惠、片商交換券、團劃包廳、影城免費票恕不適用。"
	);

	/** 單獨出現即可判定為 SPECIAL_BY_DATE 的警語。 */
	private static final Pattern SPECIAL_BY_DATE_CLUE = Pattern.compile(
	        "特別場贈品統一於活動當日入場時發放"
	);
    /** 威秀一般週特典公告固定罐頭文的辨識條件 */
    private static final Pattern REGULAR_BOILERPLATE = Pattern.compile(
            "特典領取注意事項"
                    + "|使用APP訂票並選擇電子票券者"
                    + "|非該入場特典活動期間的票根不可兌換");

    /** 特別場／特殊影廳線索；不包含一般公告也會出現的兌換時間、地點用語。 */
    private static final Pattern SPECIAL_SCREENING_CLUE = Pattern.compile(
            "(?i)IMAX|4DX|搶先場|獨家放映|應援場|特別場(?!票根)");

    /** 威秀一般特典公告常見線索；各條件獨立命中即可。 */
    private static final Pattern BENEFIT_ANNOUNCEMENT_CLUE = Pattern.compile(
            "起任一版本電影票"
                    + "|白天營業時間起"
                    + "|原購票影城"
                    + "|原觀影影城"
                    + "|即可至原購票影城兌換領取"
                    + "|凡購買.{0,30}?(?:20\\d{2}\\s*[年./-]\\s*)?"
                    + "\\d{1,2}\\s*(?:月|/|\\.)\\s*\\d{1,2}\\s*日?");

    /** 入場時發放型的主要線索，也是特別場罐頭文的一部分。 */
    private static final Pattern ADMISSION_GIFT_CLUE = Pattern.compile(
            "贈品統一於活動當日入場時發放"
                    + "|特別場贈品統一於活動當日入場時發放");

    @Override
    public boolean supports(String username, String sourceUrl) {
        String user = username == null ? "" : username.toLowerCase();
        String url = sourceUrl == null ? "" : sourceUrl.toLowerCase();

        return user.contains("vieshow")
                || url.contains("facebook.com/vieshow");
    }

    @Override
    public boolean hasSpecialBoilerplate(String originalText) {
        return originalText != null
                && SPECIAL_BOILERPLATE.matcher(originalText).find();
    }

    @Override
    public boolean hasRegularBoilerplate(String originalText) {
        return originalText != null
                && REGULAR_BOILERPLATE.matcher(originalText).find();
    }

    @Override
    public boolean isBenefitAnnouncement(String originalText) {
        return originalText != null
                && BENEFIT_ANNOUNCEMENT_CLUE.matcher(originalText).find();
    }

    /** 是否有特殊影廳或特殊場次線索。 */
    public boolean hasSpecialScreeningClue(String originalText) {
        return originalText != null
                && SPECIAL_SCREENING_CLUE.matcher(originalText).find();
    }

    /**
     * 判斷威秀特別場的BenefitType
     * 命中入場贈送型固定警語時，歸類為SPECIAL_BY_DATE。
     * 否則，若有特殊場次線索，暫歸類為SPECIAL_UNTIL_OUT。
     * 此方法不解析特典名稱、日期或影城等欄位。
     */
    public Optional<BenefitType> classifySpecialScreening(String originalText) {
        if (originalText == null || originalText.isBlank()) {
            return Optional.empty();
        }

        if (ADMISSION_GIFT_CLUE.matcher(originalText).find()) {
            return Optional.of(BenefitType.SPECIAL_BY_DATE);
        }

        if (hasSpecialScreeningClue(originalText)) {
            return Optional.of(BenefitType.SPECIAL_UNTIL_OUT);
        }

        return Optional.empty();
    }

}
