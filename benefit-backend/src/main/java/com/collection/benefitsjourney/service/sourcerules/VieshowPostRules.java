package com.collection.benefitsjourney.service.sourcerules;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;

/**
 * 威秀影城貼文規則。
 *
 * 重點：貼文實際用的是「半形」符號（/ ( ) :），而且每行前面常有 ⭕️❌⚠️✦ 之類的 emoji，
 * 所以這裡一律「先把全形轉半形，再逐行用 contains 找關鍵句」，不再用多行 regex 硬串。
 */
@Component
public class VieshowPostRules implements BenefitPostRules {

    // ---------- 一般週特典 ----------
    /** 週特典公告一定有的一句話：「凡購買…期間…於…白天營業時間起至原購票影城，即可兌換領取」 */
    private static final String WEEKLY_KEY_1 = "凡購買";
    private static final Pattern WEEKLY_KEY_2 = Pattern.compile("原購票影城|白天營業時間起");

    /** 週特典罐頭文（單獨出現也算一般週特典） */
    private static final List<String> REGULAR_BOILERPLATE_KEYS = List.of(
            "特典領取注意事項",
            "使用APP訂票並選擇電子票券者",
            "非該入場特典活動期間的票根不可兌換");

    // ---------- 特別場 ----------
    /** 特別場罐頭文：三句都要有 */
    private static final List<String> SPECIAL_BOILERPLATE_KEYS = List.of(
            "愛心票/敬老票僅限",
            "除以上購票通路",
            "團券加價、銀行優惠");

    /** 入場贈送型的線索（任一句命中） */
    private static final Pattern SPECIAL_BY_DATE_CLUE = Pattern.compile(
            "贈品統一於活動當日入場時發放|統一於活動當日入場時發放|入場即可獲得");

    /** 特別場貼文裡「有講到特典」的線索；沒有它就是資訊不完整 */
    private static final Pattern SPECIAL_GIFT_HINT = Pattern.compile(
            "本場限定|入場即可獲得|特別場套組|特別場限定|場次限定");

    /** 特別場／特殊影廳線索（特別場票根 是罐頭文裡的字，要排除） */
    private static final Pattern SPECIAL_SCREENING_CLUE = Pattern.compile(
            "(?i)IMAX|4DX|搶先場|獨家放映|應援場|特別場(?!票根)");

    // ---------- 要排除的 ----------
    /** 每週懶人包：只有「懶人包」，不能用「開箱」，因為真正的週特典貼文也會寫「搶先開箱」 */
    private static final Pattern ROUNDUP = Pattern.compile("懶人包");

    /** 更換公告（贈品瑕疵重印）：不是新特典 */
    private static final Pattern REPLACE_NOTICE = Pattern.compile("特典更換公告|更換資訊|更換期間");

    /** 「活動限定入場特典：即將公開」→ 特典還沒公布，資訊不完整 */
    private static final Pattern NOT_ANNOUNCED_YET = Pattern.compile("特典\\s*:\\s*(?:即將公開|敬請期待)");

    /** removeBoilerplate 用：整行只要包含任一句就當罐頭文丟掉 */
    private static final List<String> BOILERPLATE_LINE_KEYS = List.of(
            "愛心票/敬老票僅限",
            "除以上購票通路",
            "團券加價、銀行優惠",
            "特別場票根不適用",
            "贈品統一於活動當日入場時發放",
            "贈品領取後如需退票",
            "退票需連同贈品一併辦理",
            "保有活動修改、取消及最終解釋之權利",
            "特典領取注意事項",
            "使用APP訂票並選擇電子票券者",
            "非該入場特典活動期間的票根不可兌換",
            "贈品以實物為主",
            "贈品發放以現場兌換順序為主",
            "電影交換券適用本活動",
            "「敬老/愛心票」者",
            "「會員」專區",
            "分票之購票交易",
            "注意事項");

    @Override
    public boolean supports(String sourceUrl) {
        String url = sourceUrl == null ? "" : sourceUrl.toLowerCase();
        return url.contains("facebook.com/vieshow");
    }

    @Override
    public Classification classifyPost(String originalText) {
        String t = normalize(originalText);
        if (t.isBlank() || ROUNDUP.matcher(t).find() || REPLACE_NOTICE.matcher(t).find()) {
            return Classification.NOT_BENEFIT;
        }
        if (NOT_ANNOUNCED_YET.matcher(t).find()) {
            return Classification.INCOMPLETE;
        }
        if (isWeekly(t)) {
            return Classification.BENEFIT;
        }
        if (SPECIAL_BY_DATE_CLUE.matcher(t).find() || hasSpecialBoilerplate(t)) {
            // 特別場：有講到贈品才算完整；導演映後場、應援場沒講贈品 → INCOMPLETE
            return SPECIAL_GIFT_HINT.matcher(t).find() ? Classification.BENEFIT : Classification.INCOMPLETE;
        }
        if (hasRegularBoilerplate(t)) {
            return Classification.BENEFIT;
        }
        return Classification.NOT_BENEFIT;
    }

    @Override
    public Optional<BenefitType> classifyBenefitType(String originalText) {
        String t = normalize(originalText);
        if (t.isBlank()) {
            return Optional.empty();
        }
        if (isWeekly(t)) {
            return Optional.of(BenefitType.REGULAR);
        }
        if (SPECIAL_BY_DATE_CLUE.matcher(t).find()) {
            return Optional.of(BenefitType.SPECIAL_BY_DATE);
        }
        if (hasSpecialScreeningClue(t)) {
            return Optional.of(BenefitType.SPECIAL_UNTIL_OUT);
        }
        if (hasRegularBoilerplate(t)) {
            return Optional.of(BenefitType.REGULAR);
        }
        return Optional.empty();
    }

    @Override
    public String removeBoilerplate(String originalText) {
        String t = normalize(originalText);
        if (t.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : t.split("\\R")) {
            if (BOILERPLATE_LINE_KEYS.stream().anyMatch(line::contains)) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    @Override
    public boolean hasSpecialBoilerplate(String originalText) {
        String t = normalize(originalText);
        return SPECIAL_BOILERPLATE_KEYS.stream().allMatch(t::contains);
    }

    @Override
    public boolean hasRegularBoilerplate(String originalText) {
        String t = normalize(originalText);
        return REGULAR_BOILERPLATE_KEYS.stream().anyMatch(t::contains);
    }

    @Override
    public boolean hasSpecialScreeningClue(String originalText) {
        return SPECIAL_SCREENING_CLUE.matcher(normalize(originalText)).find();
    }

    private boolean isWeekly(String normalized) {
        return normalized.contains(WEEKLY_KEY_1) && WEEKLY_KEY_2.matcher(normalized).find();
    }

    /** 全形轉半形：貼文用半形，程式碼裡的比對字串也統一用半形 */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('／', '/').replace('（', '(').replace('）', ')')
                .replace('：', ':').replace('～', '~').replace('　', ' ');
    }
}
