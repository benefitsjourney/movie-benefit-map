package com.collection.benefitsjourney.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 把貼文寫的影城簡稱（例如「台北信義」「南港LaLaport」），對應到資料庫影城名稱清單。
 * 簡稱通常是資料庫名稱的一部分，所以用「資料庫名稱包含簡稱」判斷。
 * 同一個簡稱對到多間時直接放進unresolved讓人工處理。
 * MUVIE CINEMAS開頭的名稱另外處理：去掉品牌前綴後比對，還是對不到就用「縣市相同的唯一一間MUVIE」。
 */
final class CinemaNameMatcher {

    private static final String MUVIE = "muviecinemas";

    private CinemaNameMatcher() {
    }

    /** indexes＝對到的資料庫影城位置（不重複）；unresolved＝找不到或不確定的簡稱 */
    record Match(List<Integer> indexes, List<String> unresolved) {
    }

    static Match match(List<String> tokens, List<String> dbNames) {
        List<Integer> indexes = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        if (tokens == null) {
            return new Match(indexes, unresolved);
        }
        for (String token : tokens) {
            String simple = MovieNameMatcher.simplify(token);
            boolean muvie = simple.startsWith(MUVIE);
            String t = stripChainSuffix(muvie ? simple.substring(MUVIE.length()) : simple);
            if (t.length() < 2) {
                continue;
            }
            List<Integer> hits = new ArrayList<>();
            for (int i = 0; i < dbNames.size(); i++) {
                if (MovieNameMatcher.simplify(dbNames.get(i)).contains(t)) {
                    hits.add(i);
                }
            }
            if (hits.isEmpty() && muvie) {
                hits = muvieSameCounty(t, dbNames);
            }
            if (hits.size() > 1) {
                // 多間都像：只接受「去掉威秀影城後完全相同」的那一間
                final String key = t;
                List<Integer> exact = hits.stream()
                        .filter(i -> stripChainSuffix(MovieNameMatcher.simplify(dbNames.get(i))).equals(key))
                        .toList();
                hits = exact.size() == 1 ? exact : List.of();
            }
            if (hits.size() == 1) {
                if (!indexes.contains(hits.get(0))) {
                    indexes.add(hits.get(0));
                }
            } else {
                unresolved.add(token);
            }
        }
        return new Match(indexes, unresolved);
    }

    /** 資料庫名稱有MUVIE CINEMAS，且去掉前綴後開頭的縣市兩個字跟簡稱相同的影城 */
    private static List<Integer> muvieSameCounty(String t, List<String> dbNames) {
        List<Integer> result = new ArrayList<>();
        if (t.length() < 2) {
            return result;
        }
        String county = t.substring(0, 2);
        for (int i = 0; i < dbNames.size(); i++) {
            String db = MovieNameMatcher.simplify(dbNames.get(i));
            if (db.startsWith(MUVIE) && db.substring(MUVIE.length()).startsWith(county)) {
                result.add(i);
            }
        }
        return result;
    }

    /** 去掉結尾的「威秀影城」「威秀」「影城」 */
    static String stripChainSuffix(String simplified) {
        String s = simplified;
        for (String suffix : new String[] { "威秀影城", "影城", "威秀" }) {
            if (s.endsWith(suffix) && s.length() > suffix.length()) {
                s = s.substring(0, s.length() - suffix.length());
            }
        }
        return s;
    }
}