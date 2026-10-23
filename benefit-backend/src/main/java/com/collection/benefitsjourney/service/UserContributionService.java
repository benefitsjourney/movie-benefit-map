package com.collection.benefitsjourney.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.UserContributionRankingResponse;
import com.collection.benefitsjourney.dto.UserContributionRankingResponse.RankingEntry;
import com.collection.benefitsjourney.repository.UserContributionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 使用者貢獻排行榜
 * 貢獻＝庫存回報（新增時記一筆）＋缺漏特典回報（審核通過時記一筆）
 * 次數不另外存，每次查詢時依 created_at 區間即時統計
 */
@Service
@RequiredArgsConstructor
public class UserContributionService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    /** 「全部」用一個很早的日期當起點，查詢就不用另外寫一支 */
    private static final LocalDateTime ALL_TIME_START = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final UserContributionRepository userContributionRepository;

    @Transactional(readOnly = true)
    public UserContributionRankingResponse getRanking(String period, Integer limit, Integer userId) {

        // 1. 決定統計區間
        String key = period == null ? "weekly" : period.trim().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        LocalDateTime since = switch (key) {
            case "weekly" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case "monthly" -> today.withDayOfMonth(1).atStartOfDay();
            case "all" -> null;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "period只能是weekly、monthly或all");
        };

        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));

        // 2. 依會員統計次數（已依總次數排好）
        List<Object[]> rows = userContributionRepository
                .countByUserSince(since == null ? ALL_TIME_START : since);

        // 3. 算名次：次數一樣就同名次（例如 1、2、2、4）
        List<RankingEntry> all = new ArrayList<>();
        long previousTotal = -1;
        int rank = 0;
        long totalContributions = 0;

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            Integer usersId = (Integer) row[0];
            long inventory = toLong(row[3]);
            long benefitReport = toLong(row[4]);
            long total = toLong(row[5]);

            if (total != previousTotal) {
                rank = i + 1;
                previousTotal = total;
            }
            totalContributions += total;

            all.add(new RankingEntry(
                    rank,
                    usersId,
                    displayName(usersId, (String) row[1], (String) row[2]),
                    total,
                    inventory,
                    benefitReport));
        }

        // 4. 有帶 userId 就找出他的名次（排行榜是公開資料，所以不用登入也能查）
        RankingEntry me = null;
        if (userId != null) {
            me = all.stream()
                    .filter(entry -> userId.equals(entry.usersId()))
                    .findFirst()
                    .orElse(null);
        }

        return new UserContributionRankingResponse(
                key,
                since,
                all.size(),
                totalContributions,
                List.copyOf(all.subList(0, Math.min(size, all.size()))),
                me);
    }

    /** 顯示名稱：暱稱 → 帳號 → 會員#編號（公開頁面，不顯示 Email） */
    private static String displayName(Integer usersId, String name, String account) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (account != null && !account.isBlank()) {
            return account.trim();
        }
        return "會員#" + usersId;
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
