package com.collection.benefitsjourney.service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * 特典名稱整理，分成兩種用途：
 * 1. forStorage：存進資料庫、顯示在地圖上的名稱，只去掉明顯多餘的部分（外層括號、開頭的#、週次標籤、開頭的《片名》）。
 * 2. forMatching：判斷兩筆是不是同一款特典用的比對名稱，不存進資料庫。
 * 例：威秀「「破曉前行」款A3海報」和秀泰「破曉前行款 A3 海報」比對名稱都是「破曉前行海報」。
 */
public final class BenefitNameNormalizer {

    private BenefitNameNormalizer() {
    }

    /** 開頭的週次標籤，例如「第二週觀影特典｜」「第11週特典：」「首週特典｜」 */
    private static final Pattern WEEK_LABEL = Pattern.compile(
            "^(?:第\\s*[0-9一二三四五六七八九十]+\\s*週|首週|最終)[^｜|:：]{0,8}[｜|:：]\\s*");

    /** 開頭的《片名》，後面還有其他字才拿掉 */
    private static final Pattern LEADING_TITLE = Pattern.compile("^《[^》]*》\\s*(?=\\S)");

    /** 結尾的數量，例如「1張」「2組」 */
    private static final Pattern TAIL_QUANTITY = Pattern.compile("[0-9一二兩]+\\s*[張個份組套本入枚]$");

    /** 比對時要拿掉的修飾字（英文已轉小寫） */
    private static final String[] MODIFIERS = { "日本進口", "限量", "獨家", "特典", "款", "a3", "a4", "a5" };

    /** 存進資料庫用：輕度整理，其他照貼文寫的 */
    public static String forStorage(String name) {
        if (name == null) {
            return null;
        }
        String s = name.trim();
        String before;
        do {
            before = s;
            s = s.replaceFirst("^[#＃]+\\s*", "");
            s = WEEK_LABEL.matcher(s).replaceFirst("");
            s = LEADING_TITLE.matcher(s).replaceFirst("");
            s = stripOuter(s, '【', '】');
            s = stripOuter(s, '「', '」');
            s = s.trim();
        } while (!s.equals(before));
        return s.isEmpty() ? name.trim() : s;
    }

    /** 比對用：花體字、全形轉一般字，英文小寫，去掉符號、空白、週次標籤、修飾字、結尾數量 */
    public static String forMatching(String name) {
        if (name == null) {
            return "";
        }
        String s = Normalizer.normalize(name, Normalizer.Form.NFKC).toLowerCase().trim();
        s = s.replaceAll("[「」【】『』《》#]", "");
        s = WEEK_LABEL.matcher(s).replaceFirst("");
        s = s.replaceAll("\\s+", "");
        s = TAIL_QUANTITY.matcher(s).replaceFirst("");
        for (String m : MODIFIERS) {
            s = s.replace(m, "");
        }
        StringBuilder sb = new StringBuilder();
        s.codePoints().filter(Character::isLetterOrDigit).forEach(sb::appendCodePoint);
        return sb.toString();
    }

    /** 名稱很像：比對名稱不同，但一個包含另一個，而且短的那個至少4個字 */
    public static boolean isSimilar(String matchingA, String matchingB) {
        if (matchingA == null || matchingB == null || matchingA.equals(matchingB)) {
            return false;
        }
        String shorter = matchingA.length() <= matchingB.length() ? matchingA : matchingB;
        String longer = shorter == matchingA ? matchingB : matchingA;
        return shorter.codePointCount(0, shorter.length()) >= 4 && longer.contains(shorter);
    }

    /** 整個名稱被同一組括號包住才拿掉，例如「【電玩概念款A3海報特典】」 */
    private static String stripOuter(String s, char open, char close) {
        if (s.length() >= 2 && s.charAt(0) == open && s.charAt(s.length() - 1) == close
                && s.indexOf(close) == s.length() - 1) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
