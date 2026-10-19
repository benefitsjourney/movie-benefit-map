package com.collection.benefitsjourney.service.sourcerules;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.TextFold;

/**
 * 威秀影城貼文規則。
 *
 * 威秀貼文實際用的是「半形」符號（/ ( ) :），而且每行前面常有⭕️❌⚠️✦之類的 emoji，
 * 所以這裡一律「先把全形轉半形，再逐行用contains找關鍵句」，不用多行Regex。
 */
@Component
public class VieshowPostRules implements BenefitPostRules {

    // ---------- 一般週特典 ----------
    /** 週特典公告一定有的一句話：「凡購買…期間…於…白天營業時間起至原購票影城，即可兌換領取」 */
    private static final Pattern WEEKLY_KEY_1 = Pattern.compile("凡?購買[^\\n]{0,40}期間|凡購買");
    private static final Pattern WEEKLY_KEY_2 = Pattern.compile("原購票影城|白天營業時間起");
    /** 另一種週特典公告格式：「兌換資訊／兌換資格／特典內容：【…】／贈品內容：…」，不一定有「凡購買」 */
    private static final Pattern EXCHANGE_INFO = Pattern.compile("兌換資訊|兌換資格|特典內容\\s*:|贈品內容\\s*:");

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
            "贈品統一於活動當日入場時發放|統一於活動當日入場時發放|入場(?:時)?即可獲得|觀影當天|專屬特典\\s*:\\s*「");

    /** 特別場貼文裡「有講到贈品」的線索，沒有就是資訊不完整 */
    private static final Pattern SPECIAL_GIFT_HINT = Pattern.compile(
            "本場限定|入場(?:時)?即可獲得|特別場套組|特別場限定|場次限定|首映限定特典|專屬特典\\s*:\\s*「"
                    + "|【#?獨家】|獨家設計工藝特典");

    /** 特別場／特殊影廳線索（「特別場票根」是罐頭文裡的字要排除） */
    private static final Pattern SPECIAL_SCREENING_CLUE = Pattern.compile(
            "(?i)IMAX|4DX|搶先場|獨家放映|應援場|特別場(?!票根)");

    /** 限量兌換型特別場：「…即可臨櫃兌換「贈品」」「…即可於入場前領取「贈品」」（週特典與入場贈送型已先被判別掉） */
    private static final Pattern REDEEM_GIFT = Pattern.compile("(?:兌換|領取|保證獲得|即可獲得)[^「【\\n]{0,4}[「【][^」】]+[」】]");

    /** 預購禮：「預購…電影票即可獲得…」「預購禮」「預售專屬特典」（不含套餐贈品） */
    private static final Pattern PREORDER = Pattern.compile(
            "(?:預購|預售)[^\\n]{0,30}?(?:即可(?:獲得|領取|兌換)|可獲得|保證獲得)|預購禮|預購粉絲禮|預售專屬");

    /** 「凡購買上映期間…」＝整個檔期都能領，沒有一週一週的起訖日 */
    private static final Pattern WHOLE_RUN = Pattern.compile("凡購買.{0,6}上映期間");

    // ---------- 要排除的 ----------
    /** 每週懶人包：只有「懶人包」不能用「開箱」，因為真正的週特典貼文也會寫「搶先開箱」 */
    private static final Pattern ROUNDUP = Pattern.compile("懶人包|本週特典開箱");

    /** 更換公告（特典瑕疵重印）：不是新特典 */
    private static final Pattern REPLACE_NOTICE = Pattern.compile("特典更換公告|更換公告|更換資訊|更換期間|換貨資訊|換貨方式");

    /** 「活動限定入場特典：即將公開」→特典還沒公布，資訊不完整 */
    private static final Pattern NOT_ANNOUNCED_YET = Pattern.compile("特典\\s*:\\s*(?:即將公開|敬請期待)");

    /** VR演唱會／直播／抽獎／猜謎／見面會這類活動貼文：就算提到「特典」「明信片」，也不是電影票特典，不要塞進審核 */
    private static final Pattern NOISE = Pattern.compile(
            "VR\\s*(?:演唱會|CONCERT)|零秒差直播|現場直播|直播|Quiz|抽獎|得獎名單|生日快樂|倒數\\s*\\d|見面會|映後|同樂會|應援",
            Pattern.CASE_INSENSITIVE);

    /** removeBoilerplate用：整行只要包含任一句就當罐頭文丟掉 */
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
        if (isWeekly(t) && !SPECIAL_BY_DATE_CLUE.matcher(t).find()) {
            return Classification.BENEFIT;
        }
        if (SPECIAL_BY_DATE_CLUE.matcher(t).find() || hasSpecialBoilerplate(t)) {
            // 特別場：有講到特典才算完整；導演映後場、應援場沒講特典→有特典字樣才送審核，否則當雜訊
            if (SPECIAL_GIFT_HINT.matcher(t).find()) {
                return Classification.BENEFIT;
            }
            return looksLikeBenefit(t) ? Classification.INCOMPLETE : Classification.NOT_BENEFIT;
        }
        if (REDEEM_GIFT.matcher(t).find() && !NOISE.matcher(t).find()) {
            return Classification.BENEFIT;
        }
        if (isPreorderBenefit(t)) {
            return Classification.BENEFIT;
        }
        if (hasRegularBoilerplate(t)) {
            return Classification.BENEFIT;
        }
        // 前面都沒命中，但像是特典也送審核
        return looksLikeBenefit(t) ? Classification.INCOMPLETE : Classification.NOT_BENEFIT;
    }

    // ---------- 疑似特典（沒被判成特典，但最好人工檢查） ----------
    /** 特典關鍵字 */
    private static final Pattern SUSPECT_KEY = Pattern.compile("特典|預購禮|粉絲禮|觀影禮|隨票贈|贈品|影迷限定");
    /** 看起來是實體贈品的字 */
    private static final Pattern SUSPECT_ITEM = Pattern.compile("海報|貼紙|明信片|小卡|卡片|照片卡|吊飾|公仔|毛巾|立牌|徽章|別針|盲包|套組|A3|A5");

    /**
     * 有特典關鍵字＋有贈品字樣才算「疑似特典」。以下情況不算：
     * ‧iShow會員抽獎（不是電影票特典）、揪團／業務部專屬（一般觀眾領不到）
     * ‧預售情報（和首映貼文重複）、買套餐送的（不是電影票特典，例如芬達收納袋）
     */
    private boolean looksLikeBenefit(String normalized) {
        String squeezed = normalized.replaceAll("\\s+", "");
        if (squeezed.contains("iShow會員") || squeezed.contains("預售情報") || squeezed.contains("套餐")
                || NOISE.matcher(normalized).find()) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : normalized.split("\\R")) {
            if (!line.contains("揪團")) {
                sb.append(line).append('\n');
            }
        }
        String t = sb.toString();
        return SUSPECT_KEY.matcher(t).find() && SUSPECT_ITEM.matcher(t).find();
    }

    @Override
    public String incompleteNote(String originalText) {
        String t = normalize(originalText);
        if (t.isBlank() || ROUNDUP.matcher(t).find() || REPLACE_NOTICE.matcher(t).find()
                || NOT_ANNOUNCED_YET.matcher(t).find()) {
            return null;
        }
        boolean plain = !(isWeekly(t) || SPECIAL_BY_DATE_CLUE.matcher(t).find() || hasSpecialBoilerplate(t)
                || REDEEM_GIFT.matcher(t).find() || isPreorderBenefit(t) || hasRegularBoilerplate(t));
        return plain && looksLikeBenefit(t) ? "疑似特典（有特典關鍵字，但格式不符無法自動解析），請人工確認" : null;
    }

    @Override
    public boolean isWholeRunBenefit(String normalizedBody) {
        return normalizedBody != null && WHOLE_RUN.matcher(normalizedBody).find();
    }

    @Override
    public Optional<BenefitType> classifyBenefitType(String originalText) {
        String t = normalize(originalText);
        if (t.isBlank()) {
            return Optional.empty();
        }
        if (isWeekly(t) && !SPECIAL_BY_DATE_CLUE.matcher(t).find()) {
            return Optional.of(WHOLE_RUN.matcher(t).find() ? BenefitType.SPECIAL_UNTIL_OUT : BenefitType.REGULAR);
        }
        if (SPECIAL_BY_DATE_CLUE.matcher(t).find()) {
            return Optional.of(BenefitType.SPECIAL_BY_DATE);
        }
        if (REDEEM_GIFT.matcher(t).find() || isPreorderBenefit(t)) {
            return Optional.of(BenefitType.SPECIAL_UNTIL_OUT);
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

    @Override
    public String chainCinemaKeyword() {
        return "威秀";
    }

    @Override
    public Map<String, String> cinemaAliases() {
        return Map.of(
                "MITSUI OUTLET PARK", "三井OUTLET",
                "M.O.P", "三井OUTLET",
                "TIGER CITY", "老虎城");
    }

    /** 官網公布的特殊廳影城 */
    private static final List<String> CINEMAS_4DX = List.of("台北信義", "林口三井OUTLET", "台南大遠百", "高雄大遠百");
    private static final List<String> CINEMAS_IMAX = List.of("新店裕隆城", "板橋大遠百", "新竹巨城", "台中大遠百",
            "台南南紡", "高雄大遠百", "花蓮新天堂樂園");

    /** 只提到4DX或只提到IMAX才套用，兩種都提到無法判斷，轉人工 */
    @Override
    public List<String> defaultCinemasByFormat(String normalizedBody) {
        String t = normalizedBody == null ? "" : normalizedBody.toUpperCase();
        boolean fourDx = t.contains("4DX");
        boolean imax = t.contains("IMAX");
        if (fourDx == imax) {
            return List.of();
        }
        return fourDx ? CINEMAS_4DX : CINEMAS_IMAX;
    }

    @Override
    public boolean isPreorderBenefit(String normalizedBody) {
        return normalizedBody != null && PREORDER.matcher(normalizedBody).find();
    }

    private boolean isWeekly(String normalized) {
        boolean pickup = WEEKLY_KEY_2.matcher(normalized).find();
        return pickup && (WEEKLY_KEY_1.matcher(normalized).find() || EXCHANGE_INFO.matcher(normalized).find());
    }

    /** 全形轉半形：貼文用半形，程式碼裡的比對字串也統一用半形 */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return TextFold.fold(s).replace('／', '/').replace('（', '(').replace('）', ')')
                .replace('：', ':').replace('～', '~').replace('　', ' ');
    }
}
