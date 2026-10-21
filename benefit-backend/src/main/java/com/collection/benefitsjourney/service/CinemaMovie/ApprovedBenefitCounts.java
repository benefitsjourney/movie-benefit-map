package com.collection.benefitsjourney.service.CinemaMovie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 把 COUNT 查詢的 [id, 筆數] 轉成對照表 */
final class ApprovedBenefitCounts {

    private ApprovedBenefitCounts() {
    }

    static Map<Integer, Integer> from(List<Object[]> rows) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (rows == null) {
            return counts;
        }
        for (Object[] row : rows) {
            if (row != null
                    && row.length >= 2
                    && row[0] instanceof Number id
                    && row[1] instanceof Number count) {
                counts.put(id.intValue(), count.intValue());
            }
        }
        return counts;
    }
}
