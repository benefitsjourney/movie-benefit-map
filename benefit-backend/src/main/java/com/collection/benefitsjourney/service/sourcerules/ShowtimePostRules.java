package com.collection.benefitsjourney.service.sourcerules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.TextFold;

/**
 * 秀泰影城貼文規則。
 *
 * 秀泰的貼文分三種：
 * 週特典（#秀泰隨票贈）、預售禮（#秀泰預售、預售好禮）、特別場套票（單人套票＋特典統一於入場時領取）。
 * 一篇貼文常有好幾樣特典或好幾個版本，所以splitOffers回傳true，由Parser逐筆拆開。
 * 特殊廳對照表寫在下面的常數，以後影城有變只要改這裡。
 */
@Component
public class ShowtimePostRules implements BenefitPostRules {

    // ---------- 影城與特殊廳 ----------
    /** 全台秀泰15間，用簡稱在同品牌影城裡比對 */
    static final List<String> ALL_CINEMAS = List.of("大巨蛋", "欣欣", "土城", "樹林", "站前", "文心", "麗寶", "嘉義",
            "基隆", "花蓮", "台東", "北港", "仁德", "岡山", "夢時代");

    static final List<String> CINEMAS_4DX = List.of("大巨蛋", "文心");
    static final List<String> CINEMAS_SCREENX = List.of("大巨蛋", "欣欣", "樹林", "站前", "文心", "嘉義", "岡山", "夢時代");
    static final List<String> CINEMAS_DOLBY = List.of("大巨蛋", "站前");
    static final List<String> CINEMAS_REMMI = List.of("大巨蛋");
    static final List<String> CINEMAS_LG = List.of("文心");
    static final List<String> CINEMAS_TEMPUR = List.of("文心");
    static final List<String> CINEMAS_WIDE = List.of("夢時代");
    /** INFINITY VISION是認證影城，不是廳 */
    static final List<String> CINEMAS_INFINITY = List.of("大巨蛋", "欣欣", "樹林", "文心", "站前", "嘉義", "岡山", "夢時代");

    private static final Pattern HALL_4DX = Pattern.compile("4DX");
    private static final Pattern HALL_SCREENX = Pattern.compile("SCREENX");
    private static final Pattern HALL_DOLBY = Pattern.compile("DOLBY|杜比");
    private static final Pattern HALL_REMMI = Pattern.compile("REMMI");
    private static final Pattern HALL_LG = Pattern.compile("LG\\s*MIRACLASS");
    private static final Pattern HALL_TEMPUR = Pattern.compile("TEMPUR|丹普");
    private static final Pattern HALL_WIDE = Pattern.compile("寬巨幕");
    private static final Pattern HALL_INFINITY = Pattern.compile("INFINITY\\s*VISION");

    /** 不是版本限定 */
    private static final Pattern ANY_VERSION = Pattern.compile("不限版本|任一版本|一般版本|其他版本");
    /** 「特殊版本為ULTRA 4DX、SCREENX、Dolby…」這種定義（大寫後比對） */
    private static final Pattern SPECIAL_VERSION_DEF = Pattern.compile("特殊版本為([^\\n]+)");

    // ---------- 特典類型 ----------
    /** 週特典：「#秀泰隨票贈」開頭的一律先當特典 */
    private static final Pattern WEEKLY = Pattern.compile("秀泰隨票贈");
    private static final Pattern WEEKLY_FORMAT = Pattern.compile(
            "活動辦法[\\s\\S]{0,200}?(?:原購票影城|持票根兌換)[\\s\\S]{0,200}?(?:即可獲得|即可至原購票影城兌換)");

    /** 預售禮 */
    private static final Pattern PRESALE = Pattern.compile("#秀泰預售|預售好禮|首波預售限定好禮");
    private static final Pattern PRESALE_GIFT = Pattern.compile("即送|即贈|即可獲得|兌換[^\\n]{0,12}【");

    /** 特別場套票 */
    private static final Pattern SPECIAL_PICKUP = Pattern.compile("特典統一於入場時|統一於入場時");
    private static final Pattern SPECIAL_TICKET = Pattern.compile("套票\\s*\\$");
    private static final Pattern SPECIAL_WORD = Pattern.compile("特別場|首映");

    // ---------- 要排除的 ----------
    /** 本週隨票贈整理：「往後滑」（資訊在圖片） */
    private static final Pattern ROUNDUP = Pattern.compile("往後滑|觀影特典一次看");
    /** 更換、補領公告 */
    private static final Pattern REPLACE_NOTICE = Pattern.compile("更換公告|發放調整公告|補領期間");
    /** 直播、轉播、演唱會：特別場判斷之前就先排除（NBA轉播也有「特典統一於入場時」） */
    private static final Pattern LIVE = Pattern.compile("直播|LIVE\\s*VIEWING|轉播|演唱會", Pattern.CASE_INSENSITIVE);
    /** 見面會、影人、抽獎、付費周邊：特別場套票本身可能附帶抽獎，所以放在特別場判斷之後 */
    private static final Pattern NOISE = Pattern.compile("見面會|影人|QA|抽獎|套餐|加購|單購");
    /** 開箱文 */
    private static final Pattern OPEN_BOX = Pattern.compile("開箱");
    /** 有活動日期：日期後面接起、期間、區間，或有日期標題 */
    private static final Pattern EVENT_DATE = Pattern.compile(
            "\\d{1,2}/\\d{1,2}(?:\\s*\\([^)]*\\))?\\s*(?:起|期間|[-~])|(?:活動日期|活動時間|放映日期|領取時間)\\s*[:｜|]");

    /** 「活動特典｜「…」」這種寫法，但不屬於三種特典 */
    private static final Pattern OTHER_GIFT = Pattern.compile("活動特典\\s*[｜|:]");

    @Override
    public boolean supports(String sourceUrl) {
        String url = sourceUrl == null ? "" : sourceUrl.toLowerCase();
        return url.contains("facebook.com/showtimecinemas");
    }

    @Override
    public Classification classifyPost(String originalText) {
        String t = normalize(originalText);
        if (t.isBlank() || ROUNDUP.matcher(t).find() || REPLACE_NOTICE.matcher(t).find()) {
            return Classification.NOT_BENEFIT;
        }
        if (isWeekly(t)) {
            return Classification.BENEFIT;
        }
        if (LIVE.matcher(t).find() || isOpenBoxOnly(t)) {
            return Classification.NOT_BENEFIT;
        }
        if (isSpecial(t)) {
            return Classification.BENEFIT;
        }
        if (NOISE.matcher(t).find()) {
            return Classification.NOT_BENEFIT;
        }
        if (isPreorderBenefit(t)) {
            return Classification.BENEFIT;
        }
        return OTHER_GIFT.matcher(t).find() ? Classification.INCOMPLETE : Classification.NOT_BENEFIT;
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
        if (isSpecial(t)) {
            return Optional.of(BenefitType.SPECIAL_BY_DATE);
        }
        if (isPreorderBenefit(t)) {
            return Optional.of(BenefitType.SPECIAL_UNTIL_OUT);
        }
        return Optional.empty();
    }

    @Override
    public String incompleteNote(String originalText) {
        String t = normalize(originalText);
        return OTHER_GIFT.matcher(t).find() && !isWeekly(t) && !isSpecial(t) && !isPreorderBenefit(t)
                ? "有活動特典，但不是週特典、特別場或預售禮，請人工確認"
                : null;
    }

    @Override
    public boolean hasSpecialScreeningClue(String originalText) {
        return isSpecial(normalize(originalText));
    }

    @Override
    public boolean hasSpecialBoilerplate(String originalText) {
        return SPECIAL_PICKUP.matcher(normalize(originalText)).find();
    }

    @Override
    public boolean isPreorderBenefit(String normalizedBody) {
        return normalizedBody != null && PRESALE.matcher(normalizedBody).find()
                && PRESALE_GIFT.matcher(normalizedBody).find();
    }

    @Override
    public String chainCinemaKeyword() {
        return "秀泰";
    }

    @Override
    public String brandName() {
        return "秀泰影城";
    }

    @Override
    public boolean splitOffers() {
        return true;
    }

    @Override
    public List<String> versionCinemas(String condition, String body) {
        String c = condition == null ? "" : normalize(condition).toUpperCase();
        if (c.isBlank() || ANY_VERSION.matcher(c).find()) {
            return null;
        }
        if (c.contains("特殊版本")) {
            var def = SPECIAL_VERSION_DEF.matcher(body == null ? "" : normalize(body).toUpperCase());
            List<String> defined = def.find() ? hallCinemas(def.group(1)) : List.of();
            return defined.isEmpty() ? union(CINEMAS_4DX, CINEMAS_SCREENX, CINEMAS_DOLBY) : defined;
        }
        List<String> halls = hallCinemas(c);
        return halls.isEmpty() ? null : halls;
    }

    @Override
    public List<String> extractCinemaNames(String text) {
        List<String> found = new ArrayList<>();
        if (text == null) {
            return found;
        }
        String t = normalize(text);
        // 依出現的順序排，同一間只留一次
        List<int[]> hits = new ArrayList<>();
        for (int i = 0; i < ALL_CINEMAS.size(); i++) {
            int at = t.indexOf(ALL_CINEMAS.get(i));
            if (at >= 0) {
                hits.add(new int[] { at, i });
            }
        }
        hits.sort((a, b) -> Integer.compare(a[0], b[0]));
        for (int[] h : hits) {
            found.add(ALL_CINEMAS.get(h[1]));
        }
        return found;
    }

    /** 一段文字提到的特殊廳，合起來的影城 */
    static List<String> hallCinemas(String upperText) {
        Set<String> out = new LinkedHashSet<>();
        if (HALL_4DX.matcher(upperText).find()) {
            out.addAll(CINEMAS_4DX);
        }
        if (HALL_SCREENX.matcher(upperText).find()) {
            out.addAll(CINEMAS_SCREENX);
        }
        if (HALL_DOLBY.matcher(upperText).find()) {
            out.addAll(CINEMAS_DOLBY);
        }
        if (HALL_REMMI.matcher(upperText).find()) {
            out.addAll(CINEMAS_REMMI);
        }
        if (HALL_LG.matcher(upperText).find()) {
            out.addAll(CINEMAS_LG);
        }
        if (HALL_TEMPUR.matcher(upperText).find()) {
            out.addAll(CINEMAS_TEMPUR);
        }
        if (HALL_WIDE.matcher(upperText).find()) {
            out.addAll(CINEMAS_WIDE);
        }
        if (HALL_INFINITY.matcher(upperText).find()) {
            out.addAll(CINEMAS_INFINITY);
        }
        return new ArrayList<>(out);
    }

    @SafeVarargs
    private static List<String> union(List<String>... lists) {
        Set<String> out = new LinkedHashSet<>();
        for (List<String> l : lists) {
            out.addAll(l);
        }
        return new ArrayList<>(out);
    }

    private boolean isWeekly(String normalized) {
        return WEEKLY.matcher(normalized).find() || WEEKLY_FORMAT.matcher(normalized).find();
    }

    private boolean isSpecial(String normalized) {
        return SPECIAL_PICKUP.matcher(normalized).find()
                || (SPECIAL_TICKET.matcher(normalized).find() && SPECIAL_WORD.matcher(normalized).find());
    }

    /** 有「開箱」但找不到活動日期：內容會在另一篇完整的售票貼文出現，略過 */
    private boolean isOpenBoxOnly(String normalized) {
        return OPEN_BOX.matcher(normalized).find() && !EVENT_DATE.matcher(normalized).find();
    }

    /** 花體字、全形英數字還原，全形符號轉半形 */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return TextFold.fold(s).replace('／', '/').replace('（', '(').replace('）', ')').replace('：', ':')
                .replace('～', '~').replace('－', '-').replace('＋', '+').replace('＄', '$').replace('　', ' ');
    }
}
