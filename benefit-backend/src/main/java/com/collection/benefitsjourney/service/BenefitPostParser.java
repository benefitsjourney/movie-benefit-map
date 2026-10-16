package com.collection.benefitsjourney.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.service.sourcerules.BenefitPostRules;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * 將Facebook公開貼文轉成候選特典資料
 * 解析不確定時一律交BenefitReport待審核。
 *
 * 1. 先把全形／特殊數字轉成半形
 * 2. 「注意事項」之後的文字不參與一般內容判斷
 * 3. 留言完全不參與解析
 * 4. 一篇貼文可解析出多個特典
 * 5. 影城專屬規則由BenefitPostRules負責
 */
@RequiredArgsConstructor
@Component
public class BenefitPostParser {

    private final List<BenefitPostRules> postRules;

    private static final Pattern FULL_DATE = Pattern.compile(
            "(?<!\\d)(20\\d{2})\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*日?");

    private static final Pattern TW_DATE =
            Pattern.compile("(?<!\\d)(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日?");

    private static final Pattern SLASH_DATE =
            Pattern.compile("(?<!\\d)(\\d{1,2})\\s*/\\s*(\\d{1,2})(?!\\d)");

    /** 日期後面緊接這些字，代表是開賣／售票日，不是特典領取日。 */
    private static final Pattern SALE_WORDS =
            Pattern.compile("開賣|售票|預售|搶票|搶購|開放購票");

    /** 兩個日期中間只有這些連接詞才算區間。 */
    private static final Pattern RANGE_GAP = Pattern.compile(
            "^\\s*(?:[（(][^)）]{1,4}[)）])?\\s*起?\\s*(?:至|到|~|-|—|–)\\s*$");

    private static final Pattern BENEFIT_KEYWORDS = Pattern.compile(
            "特典|入場禮|贈品|隨票贈|購票特典|兌換領取|兌換[「『《【]|"
                    + "入場前領取|入場即可獲得|電影限定明信片|週特典|本週入場禮|"
                    + "本週特典|送完為止|贈完為止",
            Pattern.CASE_INSENSITIVE);

    /** 沒有特典字樣時的備援：取得動作與實體物品必須同時出現。 */
    private static final Pattern ACQUIRE = Pattern.compile(
            "領取|兌換|獲得|隨票|憑票|入場即|套票|凡購買|凡購票|凡觀賞|購票即");

    private static final Pattern ITEM = Pattern.compile(
            "海報|明信片|貼紙|書籤|磁鐵|資料夾|光柵卡|劇照|紀念卡|票卡|卡組|"
                    + "立牌|杯套|吊飾|徽章|票夾|卡夾|照片卡|插圖視覺卡");

    /** 圖片型貼文：文字很少，但包含這些字時送人工審核。 */
    private static final Pattern IMAGE_HINT =
            Pattern.compile("情報|公開|活動|限定|週|開賣|上映|兌換|領取");

    private static final Pattern MEMBER = Pattern.compile(
            "會員限定|會員專屬|限會員|會員憑證|會員資格|會員禮|會員獨享|持會員卡|EDM|電子報",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern REGULAR = Pattern.compile(
            "週特典|週週特典|首週|第\\s*[一二三四五六七八九十\\d]+\\s*週|本週入場禮|本週特典");

    private static final Pattern UNTIL_OUT =
            Pattern.compile("送完為止|售完為止|數量有限|贈完為止");

    private static final Pattern SPECIAL =
            Pattern.compile("特別場|特殊場|映後|映後座談|見面會|首映會|應援場|主題場|回歸場|返場|再上映|重映|紀念場|追加場");

    private static final Pattern EXPLICIT_BENEFIT_NAME = Pattern.compile(
            "(?:特典名稱|特典名|贈品名稱|入場禮|入場特典)\\s*[：:為是]\\s*[「『《【]?([^\\n\\r，。！？!?]{2,80})");

    private static final Pattern GIFT_TRIGGER =
            Pattern.compile("兌換領取|即可領取|即可獲得|入場前領取|領取|獲得");

    /** 「名稱」或【名稱】，後面可接「款A3海報1張」這種補充。 */
    private static final Pattern BRACKET_ITEM = Pattern.compile(
            "[「『【]\\s*([^」』】\\n]{1,80}?)\\s*[」』】](款[^\\s，,。！!?+＋、「」『』【】]{1,20})?");

    private static final Pattern LABELED_QUOTED = Pattern.compile(
            "特典(?:情報|名稱)?\\s*[：:]\\s*[^「『【\\n]{0,40}?[「『【]\\s*([^」』】\\n]{2,80}?)\\s*[」』】]");

    private static final Pattern PACK_ITEM = Pattern.compile(
            "\\d+\\s*[組張份套]\\s*[「『【]\\s*([^」』】\\n]{2,60}?)\\s*[」』】]");

    private static final Pattern WEEK_NUMBER =
            Pattern.compile("第\\s*([一二三四五六七八九十\\d]+)\\s*週");

    private static final Pattern MOVIE_TITLE =
            Pattern.compile("《([^》\\n]{1,60})》");

    /** 相容舊呼叫：只回傳第一筆。 */
    public ParsedBenefitPost parse(JsonNode item) {
        return parseAll(item).get(0);
    }

    /** 整理原始貼文資料：從JSON中讀取必要資訊，並進行全半形符號等格式處理。一篇貼文解析成一筆以上候選特典，非特典貼文也會回傳一筆。 */
    public List<ParsedBenefitPost> parseAll(JsonNode item) {
        String originalText = normalize(item.path("text").asString("")).trim();

        String sourceUrl = firstNonBlank(
                item.path("url").asString(""),
                item.path("permalink").asString(""));

        String postId = firstNonBlank(
                item.path("postId").asString(""),
                firstNonBlank(
                        item.path("id").asString(""),
                        item.path("code").asString("")));

        if (postId.isBlank() && !sourceUrl.isBlank()) {
            int slash = sourceUrl.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < sourceUrl.length()) {
                postId = sourceUrl.substring(slash + 1).split("[?#]")[0];
            }
        }

        String username = firstNonBlank(
                item.path("username").asString(""),
                firstNonBlank(
                        item.path("pageName").asString(""),
                        firstNonBlank(
                                item.path("page_name").asString(""),
                                firstNonBlank(
                                        item.path("author").asString(""),
                                        item.path("profileName").asString("")))));

        LocalDateTime publishedAt = parseTimestamp(firstNonBlank(
                item.path("createdAt").asString(""),
                firstNonBlank(
                        item.path("created_at").asString(""),
                        firstNonBlank(
                                item.path("date").asString(""),
                                firstNonBlank(
                                        item.path("time").asString(""),
                                        item.path("timestamp").asString(""))))));

        List<String> images = extractImageUrls(item);

        // 由來源規則判斷貼文所屬影城
        BenefitPostRules sourceRule = findSourceRule(username, sourceUrl);

        // 逐行移除留言或罐頭文等不需要參與後續判斷的文字
        // 1. 移除符合影城專屬規則的固定文案
        // 2. 遇到包含「注意事項」的行就停止處理後續文字
        // 3. 移除特定通用退票說明
        // 4. 保留剩下的主要公告內容
        String body = removeBoilerplate(originalText, sourceRule);

        // 判斷NOT_BENEFIT（找到關鍵字是特典）／NOT_BENEFIT（沒找到關鍵字不是特典）／INCOMPLETE（有圖片文字資訊不足無法判斷）
        ParsedBenefitPost.Classification classification =
                classify(body, images, sourceRule);

        List<String> names = classification == ParsedBenefitPost.Classification.NOT_BENEFIT
                ? List.of()
                : extractBenefitNames(body);

        DateRange range = extractDateRange(body, publishedAt);
        String benefitType = inferType(body, range);

        LocalDate endDate = range.end();

        if ("REGULAR".equals(benefitType)
                && endDate == null
                && range.start() != null) {
            endDate = range.start().plusDays(6);
        }

        String movieName = extractMovieName(body);

        String reviewNote = "爬蟲初步整理，請核對原始公告；不確定欄位請由管理員補正。";

        if (classification != ParsedBenefitPost.Classification.NOT_BENEFIT) {
            List<String> missing = new ArrayList<>();

            if (names.isEmpty()) {
                missing.add("特典名稱");
            }
            if (benefitType == null) {
                missing.add("特典類型");
            }
            if (range.start() == null) {
                missing.add("開始日期（可能在圖片或留言）");
            }

            if (!missing.isEmpty()) {
                classification = ParsedBenefitPost.Classification.INCOMPLETE;
                reviewNote = "無法可靠辨識「"
                        + String.join("、", missing)
                        + "」，請依原始公告／圖片補正。";
            }
        }

        if (originalText.isBlank() && !images.isEmpty()) {
            classification = ParsedBenefitPost.Classification.INCOMPLETE;
            reviewNote = "圖片型公告，文字不足以判斷特典名稱、日期或條件，請人工查看原圖。";
        }

        if (sourceUrl.isBlank()) {
            reviewNote = "來源貼文網址缺漏，請人工確認來源；" + reviewNote;
        }

        Boolean memberOnly = MEMBER.matcher(body).find();

        LocalDateTime startDt =
                range.start() == null ? null : range.start().atStartOfDay();

        LocalDateTime endDt =
                endDate == null ? null : endDate.atTime(23, 59, 59);

        List<String> targets = new ArrayList<>(names);
        if (targets.isEmpty()) {
            targets.add(null);
        }

        List<ParsedBenefitPost> results = new ArrayList<>();

        for (String name : targets) {
            results.add(new ParsedBenefitPost(
                    postId,
                    sourceUrl,
                    username,
                    originalText,
                    publishedAt,
                    movieName,
                    name,
                    memberOnly,
                    benefitType,
                    startDt,
                    endDt,
                    images,
                    truncate(reviewNote, 100),
                    classification));
        }

        return results;
    }

    /**
     * 找出適用來源影城的規則。
     * 如將來新增其他影城，只需新增BenefitPostRules實作。
     */
    private BenefitPostRules findSourceRule(String username, String sourceUrl) {
        return postRules.stream()
                .filter(rule -> rule.supports(username, sourceUrl))
                .findFirst()
                .orElse(null);
    }

    // ---------- 分類 ----------

    private static ParsedBenefitPost.Classification classify(
            String body,
            List<String> images,
            BenefitPostRules sourceRule) {

        if (body.isBlank()) {
            return images.isEmpty()
                    ? ParsedBenefitPost.Classification.NOT_BENEFIT
                    : ParsedBenefitPost.Classification.INCOMPLETE;
        }

        boolean strong = BENEFIT_KEYWORDS.matcher(body).find();
        boolean weak = ACQUIRE.matcher(body).find()
                && ITEM.matcher(body).find();

        if (strong || weak) {
            return ParsedBenefitPost.Classification.BENEFIT;
        }

        // 有圖片且符合影城來源規則時，文字資訊不足也交由人工確認
        if (!images.isEmpty()
                && (sourceRule != null || IMAGE_HINT.matcher(body).find())) {
            return ParsedBenefitPost.Classification.INCOMPLETE;
        }

        return ParsedBenefitPost.Classification.NOT_BENEFIT;
    }

    // ---------- 文字前處理 ----------

    /** 全形數字、特殊數字轉半形；全形斜線、連字號、波浪號轉半形。 */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(text.length());

        text.codePoints().forEach(cp -> {
            if (Character.isDigit(cp)) {
                sb.append((char) ('0' + Character.digit(cp, 10)));
            } else if (cp == '／') {
                sb.append('/');
            } else if (cp == '－') {
                sb.append('-');
            } else if (cp == '～' || cp == '〜') {
                sb.append('~');
            } else {
                sb.appendCodePoint(cp);
            }
        });

        return sb.toString();
    }

    /**
     * 1. 移除不應參與特典判斷的罐頭文
     * 2. 影城專屬罐頭文由BenefitPostRules判斷
     * 3. 通用注意事項則沿用原本的停止規則
     */
    private static String removeBoilerplate(
            String text,
            BenefitPostRules sourceRule) {

        if (text == null || text.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String line : text.split("\\R")) {
            boolean specialBoilerplate = sourceRule != null
                    && sourceRule.hasSpecialBoilerplate(line);

            boolean regularBoilerplate = sourceRule != null
                    && sourceRule.hasRegularBoilerplate(line);

            // 「特典領取注意事項」屬於來源規則識別的注意事項標題。
            if (line.contains("注意事項")) {
                break;
            }

            if (specialBoilerplate || regularBoilerplate) {
                continue;
            }

            // 不依賴特定影城的通用退票說明。
            if (line.contains("贈品領取後如需退票")
                    || line.contains("退票需連同贈品一併辦理")
                    || line.contains("退/換票需連同贈品一併辦理")) {
                continue;
            }

            sb.append(line).append('\n');
        }

        return sb.toString().trim();
    }

    
    // 從過濾後的貼文中尋找需要的欄位
    
    // ---------- 特典 ----------
    private static List<String> extractBenefitNames(String text) {
        LinkedHashSet<String> names = new LinkedHashSet<>();

        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

//        // 1. 明確標示「特典名稱：」
//        Matcher explicit = EXPLICIT_BENEFIT_NAME.matcher(text);
//
//        while (explicit.find()) {
//            String candidate = cleanName(explicit.group(1));
//
//            if (candidate == null) {
//                continue;
//            }
//
//            candidate = candidate
//                    .replaceAll("[「『《【」』》】].*$", "")
//                    .trim();
//
//            candidate = candidate
//                    .replaceAll("(數量有限|送完為止|售完為止).*$", "")
//                    .trim();
//
//            if (usableName(candidate)) {
//                names.add(candidate);
//                break;
//            }
//        }

        // 2. 「兌換領取【A】1張 +【B】1組」：擷取同一句所有括號項目
        if (names.isEmpty()) {
            Matcher trigger = GIFT_TRIGGER.matcher(text);

            while (trigger.find()) {
                String segment = text.substring(
                        trigger.end(),
                        segmentEnd(text, trigger.end()));

                Matcher item = BRACKET_ITEM.matcher(segment);

                while (item.find()) {
                    addName(names, item.group(1), item.group(2));
                }
            }
        }

        // 3. 「特典情報：…「名稱」」
        if (names.isEmpty()) {
            Matcher labeled = LABELED_QUOTED.matcher(text);

            while (labeled.find()) {
                addName(names, labeled.group(1), null);
            }
        }

        // 4. 「1 組「名稱」」（套票型貼文）
        if (names.isEmpty()) {
            Matcher pack = PACK_ITEM.matcher(text);

            while (pack.find()) {
                addName(names, pack.group(1), null);
            }
        }

        // 5. 週特典但沒寫名稱時，使用通用名稱
        if (names.isEmpty()) {
            Matcher week = WEEK_NUMBER.matcher(text);

            if (week.find() && REGULAR.matcher(text).find()) {
                names.add("第" + week.group(1) + "週入場特典");
            } else if (Pattern.compile("週特典|本週入場禮|本週特典")
                    .matcher(text).find()) {
                names.add("週入場特典");
            }
        }

        return new ArrayList<>(names);
    }

    private static void addName(
            LinkedHashSet<String> names,
            String core,
            String suffix) {

        String name = core == null ? "" : core.trim();

        if (suffix != null) {
            // 「款A3海報1張」：去掉結尾數量
            name += suffix.replaceAll("[一二三四五六七八九十\\d]+\\s*[張組份套個]$", "");
        }

        name = cleanName(name);

        if (usableName(name)) {
            names.add(name);
        }
    }

    private static boolean usableName(String candidate) {
        return candidate != null
                && candidate.length() >= 2
                && !FULL_DATE.matcher(candidate).lookingAt()
                && !TW_DATE.matcher(candidate).lookingAt()
                && !SLASH_DATE.matcher(candidate).lookingAt()
                && !candidate.matches("^(期間|活動期間|領取期間|領取方式|活動詳情|詳情|如下|即將公開|敬請期待).*$")
                && !BENEFIT_KEYWORDS.matcher(candidate).matches();
    }

    private static int segmentEnd(String text, int from) {
        int limit = Math.min(text.length(), from + 150);

        for (int i = from; i < limit; i++) {
            char c = text.charAt(i);

            if (c == '。' || c == '\n') {
                return i;
            }
        }

        return limit;
    }

    private static String cleanName(String value) {
        if (value == null) {
            return null;
        }

        String result = value.trim().replaceAll(
                "^[：:、，,\\s「『《【]+|[：:、，,\\s。！？!?」』》】]+$",
                "");

        if (result.isBlank() || result.length() > 100) {
            return null;
        }

        if (result.matches("^[\\p{So}\\p{Sk}\\p{Punct}\\s]+$")) {
            return null;
        }

        return result;
    }

    // 所屬電影名稱
    private static String extractMovieName(String text) {
        Matcher matcher = MOVIE_TITLE.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    // ---------- 類型 ----------
    /**
     * 判斷順序：週特典字樣／七天區間→特別場→數量有限
     * 「送完為止」只在注意事項之外的內文才會被看到，
     * 且不會覆蓋週特典或七天區間。
     */
    private static String inferType(String text, DateRange range) {
        if (text == null) {
            return null;
        }

        if (REGULAR.matcher(text).find()) {
            return "REGULAR";
        }

        if (range.start() != null && range.end() != null) {
            long days = ChronoUnit.DAYS.between(range.start(), range.end());

            if (days == 6 || days == 7) {
                return "REGULAR";
            }
        }

        boolean untilOut = UNTIL_OUT.matcher(text).find();

        if (SPECIAL.matcher(text).find()) {
            return untilOut ? "SPECIAL_UNTIL_OUT" : "SPECIAL_BY_DATE";
        }

        if (untilOut) {
            return "SPECIAL_UNTIL_OUT";
        }

        return null;
    }

    // ---------- 日期 ----------
    private record Hit(LocalDate date, int start, int end) {
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }

    private static DateRange extractDateRange(
            String text,
            LocalDateTime publishedAt) {

        if (text == null || text.isBlank()) {
            return new DateRange(null, null);
        }

        List<Hit> hits = new ArrayList<>();

        collectDates(hits, FULL_DATE.matcher(text), text, publishedAt);
        collectDates(hits, TW_DATE.matcher(text), text, publishedAt);
        collectDates(hits, SLASH_DATE.matcher(text), text, publishedAt);

        if (hits.isEmpty()) {
            return new DateRange(null, null);
        }

        hits.sort(Comparator.comparingInt(Hit::start));

        LocalDate start = hits.get(0).date();
        LocalDate end = null;

        if (hits.size() > 1) {
            Hit first = hits.get(0);
            Hit second = hits.get(1);

            String gap = text.substring(first.end(), second.start());

            // 只有兩個日期緊鄰，且中間是至／到／-／~ 才算區間。
            if (RANGE_GAP.matcher(gap).matches()) {
                end = second.date();

                if (end.isBefore(start)) {
                    end = end.plusYears(1);
                }
            }
        }

        return new DateRange(start, end);
    }

    private static void collectDates(
            List<Hit> hits,
            Matcher matcher,
            String text,
            LocalDateTime publishedAt) {

        while (matcher.find()) {
            if (overlaps(hits, matcher.start(), matcher.end())) {
                continue;
            }

            String after = text.substring(
                    matcher.end(),
                    Math.min(text.length(), matcher.end() + 14));

            if (SALE_WORDS.matcher(after).find()) {
                continue;
            }

            LocalDate date = matcher.groupCount() == 3
                    ? toDate(
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            Integer.parseInt(matcher.group(3)))
                    : inferDate(
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            publishedAt);

            if (date != null) {
                hits.add(new Hit(date, matcher.start(), matcher.end()));
            }
        }
    }

    private static boolean overlaps(List<Hit> hits, int start, int end) {
        for (Hit hit : hits) {
            if (start < hit.end() && end > hit.start()) {
                return true;
            }
        }

        return false;
    }

    private static LocalDate toDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException ex) {
            return null;
        }
    }

    /** 沒寫年份時，以貼文發布日為準；前後超過六個月就換年。 */
    private static LocalDate inferDate(
            int month,
            int day,
            LocalDateTime publishedAt) {

        int year = publishedAt == null
                ? Year.now().getValue()
                : publishedAt.getYear();

        LocalDate candidate = toDate(year, month, day);

        if (candidate != null && publishedAt != null) {
            LocalDate published = publishedAt.toLocalDate();

            if (candidate.isBefore(published.minusMonths(6))) {
                candidate = candidate.plusYears(1);
            } else if (candidate.isAfter(published.plusMonths(6))) {
                candidate = candidate.minusYears(1);
            }
        }

        return candidate;
    }

    /** Facebook時間為UTC時，轉成台灣時間再取日期。 */
    private static LocalDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(raw)
                    .atZoneSameInstant(ZoneId.of("Asia/Taipei"))
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(raw);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    // ---------- 圖片 ----------
    private static List<String> extractImageUrls(JsonNode item) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();

        addImageArray(urls, item.path("images"));
        addImageArray(urls, item.path("mediaUrls"));
        addImageArray(urls, item.path("imageUrls"));
        addImageArray(urls, item.path("image_urls"));
        addImageArray(urls, item.path("media_urls"));
        addFacebookMedia(urls, item.path("media"));

        addUrl(urls, item.path("imageUrl").asString(""));
        addUrl(urls, item.path("image_url").asString(""));

        return new ArrayList<>(urls);
    }

    private static void addFacebookMedia(
            LinkedHashSet<String> urls,
            JsonNode media) {

        if (media.isArray()) {
            for (JsonNode entry : media) {
                addFacebookPhoto(urls, entry);
            }
        } else if (media.isObject()) {
            addFacebookPhoto(urls, media);
        }
    }

    private static void addFacebookPhoto(
            LinkedHashSet<String> urls,
            JsonNode media) {

        // Facebook回傳的Video thumbnail是影片縮圖，不可當成特典圖片。
        // 僅接受明確標記為Photo的媒體。
        if (!"Photo".equalsIgnoreCase(media.path("__typename").asString(""))) {
            return;
        }

        JsonNode image = media.path("image");

        addUrl(urls, image.path("uri").asString(""));

        if (image.isString()) {
            addUrl(urls, image.asString());
        }

        // 部分 Photo項目沒有image.uri，才退回該Photo自己的thumbnail。
        if (!image.hasNonNull("uri") && !image.isString()) {
            JsonNode thumbnail = media.path("thumbnail");

            if (thumbnail.isObject()) {
                addUrl(urls, thumbnail.path("uri").asString(""));
            } else if (thumbnail.isString()) {
                addUrl(urls, thumbnail.asString());
            }
        }

        addUrl(urls, media.path("imageUrl").asString(""));
        addUrl(urls, media.path("image_url").asString(""));
    }

    private static void addImageArray(
            LinkedHashSet<String> urls,
            JsonNode array) {

        if (!array.isArray()) {
            return;
        }

        for (JsonNode image : array) {
            if (image.isString()) {
                addUrl(urls, image.asString());
            } else {
                String mediaType = image.path("type").asString("");

                if (!mediaType.isBlank()
                        && !mediaType.equalsIgnoreCase("image")
                        && !mediaType.equalsIgnoreCase("photo")) {
                    continue;
                }

                addUrl(urls, firstNonBlank(
                        image.path("url").asString(""),
                        firstNonBlank(
                                image.path("imageUrl").asString(""),
                                image.path("image_url").asString(""))));
            }
        }
    }

    private static void addUrl(LinkedHashSet<String> urls, String url) {
        if (url != null
                && (url.startsWith("https://") || url.startsWith("http://"))) {
            urls.add(url.trim());
        }
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank()
                ? a.trim()
                : b == null ? "" : b.trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}