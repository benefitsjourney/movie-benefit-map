package com.collection.benefitsjourney.service;

import java.util.List;

/**
 * 把爬蟲抓到的電影名對應到資料庫電影名稱清單。
 * 規則：先忽略空白與標點、英文大小寫後「完全相同」；找不到再試「互相包含」，
 * 但包含只在剛好唯一一部符合時才採用，避免誤配（例如「蜘蛛人」同時對到好幾部）。
 */
final class MovieNameMatcher {

    private MovieNameMatcher() {
    }

    /** @return 符合的電影在dbNames裡的位置；找不到或不確定回傳-1 */
    static int findIndex(String crawledName, List<String> dbNames) {
        String target = simplify(crawledName);
        if (target.length() < 2 || dbNames == null) {
            return -1;
        }

        for (int i = 0; i < dbNames.size(); i++) {
            if (target.equals(simplify(dbNames.get(i)))) {
                return i;
            }
        }

        // 第二輪：互相包含。「劇場版」「電影版」這種通用字先拿掉
        // 可避免如《航海王劇場版：…》被誤配到一部叫「劇場版」的資料
        String looseTarget = loose(target);
        int found = -1;
        for (int i = 0; i < dbNames.size(); i++) {
            String db = loose(simplify(dbNames.get(i)));
            if (db.length() >= 2 && looseTarget.length() >= 2
                    && (db.contains(looseTarget) || looseTarget.contains(db))) {
                if (found != -1) {
                    return -1;      // 有兩部以上都像，不猜
                }
                found = i;
            }
        }
        return found;
    }

    /** 拿掉通用字，只留片名本體 */
    private static String loose(String simplified) {
        return simplified.replace("劇場版", "").replace("電影版", "");
    }

    /** 只留文字與數字（中日英都保留），英文轉小寫 */
    static String simplify(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // NFKC：花體字（𝗧𝗪𝗦）、全形英數字還原成一般字，才比得到資料庫裡的名稱
        for (char c : java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}