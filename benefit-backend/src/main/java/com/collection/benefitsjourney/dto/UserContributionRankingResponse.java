package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 貢獻排行榜回傳資料
 * period：weekly（本週，從週一算）／monthly（本月，從 1 號算）／all（全部）
 * since：統計起點（all 時是 null）
 * rankings：前幾名（依 limit）
 * me：有帶 userId 時，那位會員的名次（沒有貢獻就是 null，前端顯示「還沒上榜」）
 */
public record UserContributionRankingResponse(
        String period,
        LocalDateTime since,
        int contributorCount,
        long totalContributions,
        List<RankingEntry> rankings,
        RankingEntry me) {

    /** 排行榜上的一位會員 */
    public record RankingEntry(
            int rank,
            Integer usersId,
            String displayName,
            long totalCount,
            long inventoryCount,
            long benefitReportCount) {
    }
}
