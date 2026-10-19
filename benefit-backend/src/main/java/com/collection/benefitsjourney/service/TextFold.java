package com.collection.benefitsjourney.service;

import java.text.Normalizer;

/**
 * 把貼文裡「裝飾過的英數字」還原成一般英數字。
 * 社群小編常用特殊花體字寫片名，例如 𝗧𝗪𝗦 𝗩𝗥 𝗖𝗢𝗡𝗖𝗘𝗥𝗧、𝑨𝒍𝒘𝒂𝒚𝒔 𝑳𝑨𝑳𝑰𝑺𝑨、全形的０９／２４會導致電影比對失敗。
 *
 * 只處理「數學花體英數字」和「全形英數字」，其他符號（①②③、❶、中文標點）一律不動，
 * 因為解析規則還會用到①這類符號。
 */
public final class TextFold {

    private TextFold() {
    }

    public static String fold(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> {
            boolean styled = (cp >= 0x1D400 && cp <= 0x1D7FF)        // 數學花體英數字
                    || (cp >= 0xFF10 && cp <= 0xFF19)                // 全形數字
                    || (cp >= 0xFF21 && cp <= 0xFF3A)                // 全形大寫
                    || (cp >= 0xFF41 && cp <= 0xFF5A);               // 全形小寫
            if (styled) {
                sb.append(Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFKC));
            } else {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }
}