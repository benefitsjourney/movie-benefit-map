package com.collection.benefitsjourney.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.sourcerules.BenefitPostRules;

/**
 * 把一篇貼文的文字解析成ParsedBenefitPost。
 * 只做「文字 → 結構化資料」，比對電影、去重、寫入BenefitReport由另一個Service負責。
 * 目前只支援「一般週特典」，特別場先回傳INCOMPLETE讓人工審核。
 */
@Component
public class BenefitPostParser {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final int REGULAR_DAYS = 7;

    /** 第一個《電影名》（名字前面可能有 #） */
    private static final Pattern MOVIE = Pattern.compile("《([^》]+)》");

    /**
     * 「凡購買 10/16(五)~10/22(四) 期間…」或「凡購買 10/16(五)起…」
     * 群組：1=起月 2=起日 3=迄月(可無) 4=迄日(可無)
     */
    private static final Pattern DATE_RANGE = Pattern.compile(
            "凡購買.{0,20}?(\\d{1,2})/(\\d{1,2})(?:\\([^)]*\\))?\\s*(?:~\\s*(\\d{1,2})/(\\d{1,2}))?");

    /** 「即可兌換領取「贈品名」款A3海報1張」→ 抓兌換領取到數量之間 */
    private static final Pattern GIFT = Pattern.compile(
            "兌換領取\\s*(.+?)\\s*\\d+\\s*[個張份組套款]");

    private final List<BenefitPostRules> rulesList;

    public BenefitPostParser(List<BenefitPostRules> rulesList) {
        this.rulesList = rulesList;
    }

    /**
     * @param pageUrl  粉專網址（JSON的facebookUrl），用來決定套用哪間影城的規則
     * @param postUrl  貼文網址（JSON的url），存進sourceUrl用來去重
     * @param time     JSON 的time（UTC）
     * @return 一篇貼文解析出的候選特典；沒有符合的規則、或不是特典則回傳空list
     */
    public List<ParsedBenefitPost> parseAll(String postId, String pageUrl, String postUrl,
                                            String username, String text, Instant time) {
        Optional<BenefitPostRules> found = rulesList.stream().filter(r -> r.supports(pageUrl)).findFirst();
        if (found.isEmpty() || text == null) {
            return List.of();
        }
        BenefitPostRules rules = found.get();
        Classification cls = rules.classifyPost(text);
        if (cls == Classification.NOT_BENEFIT) {
            return List.of();
        }

        LocalDateTime published = time == null ? null : LocalDateTime.ofInstant(time, TAIPEI);
        BenefitType type = rules.classifyBenefitType(text).orElse(null);
        String movie = findMovie(text);

        // 特別場、還沒公布贈品的貼文：先保留給人工
        if (cls == Classification.INCOMPLETE || type != BenefitType.REGULAR) {
            String note = cls == Classification.INCOMPLETE
                    ? "資訊不完整（尚未公布贈品或未提到贈品），請人工確認"
                    : "特別場貼文，尚未支援自動解析，請人工確認";
            return List.of(build(postId, postUrl, username, text, published, movie, null, type,
                    null, null, note, Classification.INCOMPLETE));
        }

        String body = normalize(rules.removeBoilerplate(text));
        String gift = findGift(body);
        LocalDate[] range = findDates(body, published == null ? null : published.toLocalDate());

        List<String> missing = new ArrayList<>();
        if (gift == null) missing.add("贈品名稱");
        if (range == null) missing.add("活動日期");
        if (!missing.isEmpty()) {
            return List.of(build(postId, postUrl, username, text, published, movie, gift, type,
                    null, null, "無法解析：" + String.join("、", missing), Classification.INCOMPLETE));
        }

        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end = range[1].atTime(LocalTime.of(23, 59, 59));
        return List.of(build(postId, postUrl, username, text, published, movie, gift, type,
                start, end, "爬蟲自動建立，請確認電影與日期", Classification.BENEFIT));
    }

    private ParsedBenefitPost build(String postId, String postUrl, String username, String text,
                                    LocalDateTime published, String movie, String gift, BenefitType type,
                                    LocalDateTime start, LocalDateTime end, String note, Classification cls) {
        return new ParsedBenefitPost(postId, postUrl, username, text, published, movie, gift,
                Boolean.FALSE, type == null ? null : type.name(), start, end, List.of(), note, cls);
    }

    private String findMovie(String text) {
        Matcher m = MOVIE.matcher(text);
        if (!m.find()) {
            return null;
        }
        String name = m.group(1).replace("#", "").trim();
        return name.isEmpty() ? null : name;
    }

    private String findGift(String body) {
        Matcher m = GIFT.matcher(body);
        if (!m.find()) {
            return null;
        }
        String name = m.group(1).replace("「", "").replace("」", "").trim();
        return name.isEmpty() ? null : name;
    }

    /** 回傳 {開始日, 結束日}；沒寫結束日就是開始日起 7 天（含當天）。找不到回傳 null */
    private LocalDate[] findDates(String body, LocalDate postDate) {
        Matcher m = DATE_RANGE.matcher(body);
        if (!m.find() || postDate == null) {
            return null;
        }
        LocalDate start = toDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), postDate);
        if (start == null) {
            return null;
        }
        LocalDate end;
        if (m.group(3) != null) {
            end = toDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), postDate);
            if (end == null) {
                return null;
            }
            if (end.isBefore(start)) {          // 跨年，例如 12/25 ~ 1/1
                end = end.plusYears(1);
            }
        } else {
            end = start.plusDays(REGULAR_DAYS - 1);
        }
        return new LocalDate[] { start, end };
    }

    /** 貼文日期沒有年份：先用發文年份；若算出來比發文日早超過半年，代表是明年（12 月發文講 1 月的事） */
    private LocalDate toDate(int month, int day, LocalDate postDate) {
        try {
            LocalDate d = LocalDate.of(postDate.getYear(), month, day);
            if (ChronoUnit.DAYS.between(d, postDate) > 180) {
                d = d.plusYears(1);
            }
            return d;
        } catch (RuntimeException e) {     // 例如 2/30
            return null;
        }
    }

    /** 全形數字轉半形、波浪號統一 */
    private String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            } else if (c == '～' || c == '〜') {
                sb.append('~');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
