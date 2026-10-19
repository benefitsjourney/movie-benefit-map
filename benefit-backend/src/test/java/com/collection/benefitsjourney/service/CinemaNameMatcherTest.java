package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.service.CinemaNameMatcher.Match;

class CinemaNameMatcherTest {

    private final List<String> db = List.of(
            "台北信義威秀", "MUVIE CINEMAS台北松仁威秀", "台北南港LaLaport威秀影城",
            "台南大遠百威秀影城", "台中大遠百威秀影城", "高雄大遠百威秀影城", "大遠百秀泰影城");

    @Test
    @DisplayName("簡稱是資料庫名稱的一部分（忽略大小寫與「威秀影城」字尾）")
    void matchesShortNames() {
        Match m = CinemaNameMatcher.match(List.of("台北信義", "南港LaLaPort", "台南大遠百威秀影城"), db);
        assertEquals(List.of(0, 2, 3), m.indexes());
        assertEquals(List.of(), m.unresolved());
    }

    @Test
    @DisplayName("MUVIE CINEMAS台北A13：縣市相同的唯一一間MUVIE；找不到的、對到多間的仍不猜")
    void unresolvedAndAmbiguous() {
        Match m = CinemaNameMatcher.match(List.of("MUVIE CINEMAS 台北A13", "大遠百", "MUVIE CINEMAS 高雄X"), db);
        assertEquals(List.of(1), m.indexes());
        assertEquals(List.of("大遠百", "MUVIE CINEMAS 高雄X"), m.unresolved());
    }

    @Test
    @DisplayName("同一間重複出現只算一次")
    void noDuplicates() {
        Match m = CinemaNameMatcher.match(List.of("台北信義", "台北信義威秀"), db);
        assertEquals(List.of(0), m.indexes());
    }
}
