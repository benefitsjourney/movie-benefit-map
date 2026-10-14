package com.collection.benefitsjourney.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

/**
 * 將 Facebook 公開貼文轉成候選特典資料。
 * 解析不確定時一律交 BenefitReport 待審核，不猜測日期或把電影名當成特典名。
 */
@Component
public class BenefitPostParser {

    private static final Pattern FULL_DATE = Pattern.compile(
            "(?<!\\d)(20\\d{2})\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*日?");
    private static final Pattern TW_DATE = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");
    private static final Pattern SLASH_DATE = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*/\\s*(\\d{1,2})(?!\\d)");
    private static final Pattern BENEFIT_KEYWORDS = Pattern.compile(
            "特典|入場禮|贈品|隨票贈|購票特典|兌換領取|兌換[「『《【]|入場前領取|入場即可獲得|電影限定明信片|週特典|本週入場禮|本週特典|送完為止|贈完為止",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MEMBER = Pattern.compile(
            "會員限定|會員專屬|限會員|會員憑證|會員資格|會員禮|會員獨享|持會員卡|EDM|電子報",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REGULAR = Pattern.compile(
            "週特典|第\\s*[一二三四五六七八九十\\d]+\\s*週|本週入場禮|本週特典");
    private static final Pattern UNTIL_OUT = Pattern.compile(
            "送完為止|售完為止|數量有限|贈完為止");
    private static final Pattern SPECIAL = Pattern.compile(
            "特別場|特殊場|映後|映後座談|見面會|首映會|應援場|主題場|回歸場|返場|再上映|重映");
    private static final Pattern EXPLICIT_BENEFIT_NAME = Pattern.compile(
            "(?:特典名稱|特典名|贈品名稱|入場禮|入場特典)\\s*[：:為是]\\s*[「『《【]?([^\\n\\r，。！？!？]{2,80})");
    private static final Pattern QUOTED_GIFT_NAME = Pattern.compile(
            "(?:兌換領取|即可領取|即可獲得|入場前領取|領取|獲得)\\s*[「『《【]([^」』》】\\n\\r]{2,80})[」』》】]");
    private static final Pattern WEEK_NUMBER = Pattern.compile(
            "第\\s*([一二三四五六七八九十\\d]+)\\s*週");

    public ParsedBenefitPost parse(JsonNode item) {
        String originalText = item.path("text").asString("").trim();
        String sourceUrl = firstNonBlank(item.path("url").asString(""), item.path("permalink").asString(""));
        String postId = firstNonBlank(
                item.path("postId").asString(""),
                firstNonBlank(item.path("id").asString(""), item.path("code").asString("")));
        if (postId.isBlank() && !sourceUrl.isBlank()) {
            int slash = sourceUrl.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < sourceUrl.length()) {
                postId = sourceUrl.substring(slash + 1).split("[?#]")[0];
            }
        }

        String username = firstNonBlank(item.path("username").asString(""),
                firstNonBlank(item.path("pageName").asString(""),
                        firstNonBlank(item.path("page_name").asString(""),
                                firstNonBlank(item.path("author").asString(""),
                                        item.path("profileName").asString("")))));
        LocalDateTime publishedAt = parseTimestamp(firstNonBlank(
                item.path("createdAt").asString(""),
                firstNonBlank(item.path("created_at").asString(""),
                        firstNonBlank(item.path("date").asString(""),
                                firstNonBlank(item.path("time").asString(""), item.path("timestamp").asString(""))))));
        List<String> images = extractImageUrls(item);
        String commentsText = extractCommentsText(item);
        // 留言可能補充日期或領取條件；若Actor有回傳留言，解析與比對時一併使用
        String analysisText = originalText;
        if (!commentsText.isBlank()) analysisText += "\n" + commentsText;

        String classificationText = removeBoilerplate(analysisText);
        boolean hasBenefitWords = BENEFIT_KEYWORDS.matcher(classificationText).find();
        boolean isVieshow = username.toLowerCase().contains("vieshow")
                || sourceUrl.toLowerCase().contains("facebook.com/vieshow");
        ParsedBenefitPost.Classification classification;
        if (analysisText.isBlank()) {
            classification = images.isEmpty()
                    ? ParsedBenefitPost.Classification.NOT_BENEFIT
                    : ParsedBenefitPost.Classification.INCOMPLETE;
        } else if (hasBenefitWords) {
            classification = ParsedBenefitPost.Classification.BENEFIT;
        } else if (!images.isEmpty() && isVieshow) {
            // 威秀公告有時把活動名稱、期間或領取方式放在圖片
            classification = ParsedBenefitPost.Classification.INCOMPLETE;
        } else {
            classification = ParsedBenefitPost.Classification.NOT_BENEFIT;
        }

        String benefitName = extractBenefitName(analysisText);
        String benefitType = inferType(analysisText);
        DateRange range = extractDateRange(analysisText, publishedAt);
        String reviewNote = "爬蟲初步整理，請核對原始公告；不確定欄位請由管理員補正。";

        if (classification != ParsedBenefitPost.Classification.NOT_BENEFIT) {
            List<String> missing = new ArrayList<>();
            if (benefitName == null) missing.add("特典名稱");
            if (benefitType == null) missing.add("特典類型");
            if (range.start() == null) missing.add("開始日期（可能在圖片或留言）");
            if (!missing.isEmpty()) {
                classification = ParsedBenefitPost.Classification.INCOMPLETE;
                reviewNote = "無法可靠辨識「" + String.join("、", missing) + "」，請依原始公告／圖片／留言補正。";
            }
        }
        if (originalText.isBlank() && !images.isEmpty()) {
            classification = ParsedBenefitPost.Classification.INCOMPLETE;
            reviewNote = "圖片型公告，文字不足以判斷特典名稱、日期或條件，請人工查看原圖。";
        }
        if (sourceUrl.isBlank()) {
            reviewNote = "來源貼文網址缺漏，請人工確認來源；" + reviewNote;
        }

        Boolean memberOnly = MEMBER.matcher(analysisText).find();
        return new ParsedBenefitPost(postId, sourceUrl, username, analysisText, publishedAt,
                benefitName, memberOnly, benefitType, range.start(), range.end(), images,
                truncate(reviewNote, 100), classification);
    }

    private static String removeBoilerplate(String text) {
        if (text == null || text.isBlank()) return "";
        return text.lines()
                .filter(line -> !line.contains("特典領取注意事項")
                        && !line.contains("特典注意事項")
                        && !line.contains("特別場票根不適用兌換")
                        && !line.contains("特別場票根不適用兌換其他活動")
                        && !line.contains("贈品統一於活動當日入場時發放")
                        && !line.contains("贈品領取後如需退票")
                        && !line.contains("退票需連同贈品一併辦理")
                        && !line.contains("退/換票需連同贈品一併辦理"))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

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

    private static void addFacebookMedia(LinkedHashSet<String> urls, JsonNode media) {
        if (media.isArray()) {
            for (JsonNode entry : media) addFacebookPhoto(urls, entry);
        } else if (media.isObject()) {
            addFacebookPhoto(urls, media);
        }
    }

    private static void addFacebookPhoto(LinkedHashSet<String> urls, JsonNode media) {
        // Facebook回傳的Video thumbnail是影片縮圖，不可當成特典圖片
        // 僅接受明確標記為Photo的媒體，避免把貼文網址或影片縮圖當圖片
        if (!"Photo".equalsIgnoreCase(media.path("__typename").asString(""))) return;

        JsonNode image = media.path("image");
        addUrl(urls, image.path("uri").asString(""));
        if (image.isString()) addUrl(urls, image.asString());

        // 部分Photo項目沒有image.uri，才退回該Photo自己的thumbnail
        if (!image.hasNonNull("uri") && !image.isString()) {
            JsonNode thumbnail = media.path("thumbnail");
            if (thumbnail.isObject()) addUrl(urls, thumbnail.path("uri").asString(""));
            else if (thumbnail.isString()) addUrl(urls, thumbnail.asString());
        }
        addUrl(urls, media.path("imageUrl").asString(""));
        addUrl(urls, media.path("image_url").asString(""));
    }

    private static void addImageArray(LinkedHashSet<String> urls, JsonNode array) {
        if (!array.isArray()) return;
        for (JsonNode image : array) {
            if (image.isString()) {
                addUrl(urls, image.asString());
            } else {
                String mediaType = image.path("type").asString("");
                if (!mediaType.isBlank() && !mediaType.equalsIgnoreCase("image")
                        && !mediaType.equalsIgnoreCase("photo")) continue;
                addUrl(urls, firstNonBlank(image.path("url").asString(""),
                        firstNonBlank(image.path("imageUrl").asString(""), image.path("image_url").asString(""))));
            }
        }
    }

    private static void addUrl(LinkedHashSet<String> urls, String url) {
        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) urls.add(url.trim());
    }

    private static String extractCommentsText(JsonNode item) {
        StringBuilder result = new StringBuilder();
        appendCommentTexts(result, item.path("comments"));
        appendCommentTexts(result, item.path("replies"));
        appendCommentTexts(result, item.path("latestReplies"));
        appendCommentTexts(result, item.path("topReplies"));
        return result.toString().trim();
    }

    private static void appendCommentTexts(StringBuilder result, JsonNode comments) {
        if (!comments.isArray()) return;
        for (JsonNode comment : comments) {
            String text = firstNonBlank(comment.path("text").asString(""),
                    firstNonBlank(comment.path("commentText").asString(""), comment.path("caption").asString("")));
            if (!text.isBlank()) result.append('\n').append(text.trim());
        }
    }

    private static String extractBenefitName(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher explicit = EXPLICIT_BENEFIT_NAME.matcher(text);
        while (explicit.find()) {
            String candidate = cleanName(explicit.group(1));
            if (candidate != null) {
                candidate = candidate.replaceAll("[「『《【」』》】].*$", "").trim();
                candidate = candidate.replaceAll("(數量有限|数量有限|送完為止|送完为止|售完為止|售完为止).*$", "").trim();
                if (candidate.length() >= 2
                        && !FULL_DATE.matcher(candidate).lookingAt()
                        && !TW_DATE.matcher(candidate).lookingAt()
                        && !SLASH_DATE.matcher(candidate).lookingAt()
                        && !candidate.matches("^(期間|活動期間|領取期間|領取方式|活動詳情|詳情|如下|即將公開|即将公开|敬請期待|敬请期待).*$")
                        && !BENEFIT_KEYWORDS.matcher(candidate).matches()) return candidate;
            }
        }
        Matcher quotedGift = QUOTED_GIFT_NAME.matcher(text);
        while (quotedGift.find()) {
            String candidate = cleanName(quotedGift.group(1));
            if (candidate != null
                    && !candidate.matches("^(即將公開|即将公开|敬請期待|敬请期待|詳情|详情|如下).*$")
                    && !candidate.matches("^(活動期間|活动期间|領取方式|领取方式).*$")) {
                return candidate;
            }
        }
        Matcher week = WEEK_NUMBER.matcher(text);
        if (week.find() && REGULAR.matcher(text).find()) {
            return "第" + week.group(1) + "週入場特典";
        }
        if (Pattern.compile("週特典|週週特典|本週入場禮|本週特典").matcher(text).find()) {
            return "週入場特典";
        }
        // 不把《電影名》或「電影名」直接當成特典名稱；無法辨識就送人工審核。
        return null;
    }

    private static String cleanName(String value) {
        if (value == null) return null;
        String result = value.trim().replaceAll("^[：:、，,\\s「『《【]+|[：:、，,\\s。！？!？」』》】]+$", "");
        if (result.isBlank() || result.length() > 100) return null;
        if (result.matches("^[\\p{So}\\p{Sk}\\p{Punct}\\s]+$")) return null;
        return result;
    }

    private static String inferType(String text) {
        if (text == null) return null;
        if (UNTIL_OUT.matcher(text).find()) return "SPECIAL_UNTIL_OUT";
        if (SPECIAL.matcher(text).find()) return "SPECIAL_BY_DATE";
        if (REGULAR.matcher(text).find()) return "REGULAR";
        return null;
    }

    private static LocalDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return java.time.OffsetDateTime.parse(raw).toLocalDateTime(); }
        catch (DateTimeParseException ignored) {
            try { return LocalDateTime.parse(raw); }
            catch (DateTimeParseException ignoredAgain) { return null; }
        }
    }

    private static DateRange extractDateRange(String text, LocalDateTime publishedAt) {
        if (text == null || text.isBlank()) return new DateRange(null, null);
        List<LocalDate> dates = new ArrayList<>();
        Matcher full = FULL_DATE.matcher(text);
        while (full.find()) addDate(dates, full.group(1), full.group(2), full.group(3));
        if (dates.isEmpty()) {
            int year = publishedAt == null ? Year.now().getValue() : publishedAt.getYear();
            Matcher tw = TW_DATE.matcher(text);
            while (tw.find()) addInferredDate(dates, year, tw.group(1), tw.group(2), publishedAt);
            if (dates.isEmpty()) {
                Matcher slash = SLASH_DATE.matcher(text);
                while (slash.find()) addInferredDate(dates, year, slash.group(1), slash.group(2), publishedAt);
            }
        }
        if (dates.isEmpty()) return new DateRange(null, null);
        dates.sort(LocalDate::compareTo);
        LocalDate start = dates.get(0), end = null;
        boolean hasRangeWord = Pattern.compile("至|到|～|~|—|－|期間|期間內|截止|止於|まで|至止").matcher(text).find();
        if (dates.size() > 1 && hasRangeWord) {
            end = dates.get(1);
            if (end.isBefore(start)) { LocalDate tmp = start; start = end; end = tmp; }
        } else if (REGULAR.matcher(text).find()) {
            // 明確是週特典且只寫開始日，依規格「上七天」推算第七天為結束日。
            end = start.plusDays(6);
        }
        return new DateRange(start.atStartOfDay(), end == null ? null : end.atTime(23, 59, 59));
    }

    private static void addDate(List<LocalDate> dates, String y, String m, String d) {
        try {
            LocalDate date = LocalDate.of(Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
            if (!dates.contains(date)) dates.add(date);
        } catch (RuntimeException ignored) { }
    }

    private static void addInferredDate(List<LocalDate> dates, int year, String m, String d,
            LocalDateTime publishedAt) {
        try {
            LocalDate candidate = LocalDate.of(year, Integer.parseInt(m), Integer.parseInt(d));
            if (publishedAt != null) {
                LocalDate published = publishedAt.toLocalDate();
                if (candidate.isBefore(published.minusMonths(6))) candidate = candidate.plusYears(1);
                else if (candidate.isAfter(published.plusMonths(6))) candidate = candidate.minusYears(1);
            }
            if (!dates.contains(candidate)) dates.add(candidate);
        } catch (RuntimeException ignored) { }
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a.trim() : b == null ? "" : b.trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) { }
}
