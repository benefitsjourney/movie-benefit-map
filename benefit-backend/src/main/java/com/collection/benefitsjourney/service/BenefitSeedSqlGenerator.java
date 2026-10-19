package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;

/**
 * 把「資料庫裡已經確認過的特典」產生成SqlDataInitializer可匯入的SQL（放到resources/db/data/下即可）。
 *
 * 產出兩個檔案
 * benefit.sql               特典主表
 * cinema_movie_benefit.sql  中介表。不寫死ID（每個人的資料庫ID不一樣），
 * 							 改用「電影名稱、影城名稱、特典名稱＋來源貼文＋開始日」去查 ID。
 *
 * 注意：中介表需要cinema、movie的資料先匯入，SqlDataInitializer已經調整好順序。
 */
public final class BenefitSeedSqlGenerator {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private BenefitSeedSqlGenerator() {
    }

    public record SqlFiles(String benefitSql, String cinemaMovieBenefitSql) {
    }

    public static SqlFiles generate(List<BenefitBean> benefits, List<CinemaMovieBenefitBean> links) {
        StringBuilder benefitSql = new StringBuilder("-- 由資料庫已確認的特典自動產生：特典\n");
        StringBuilder linkSql = new StringBuilder("-- 由資料庫已確認的特典自動產生：中介表\n"
                + "-- 需要cinema、movie、benefit表已有資料，找不到電影或影城時，那幾行不會寫入任何資料\n");

        for (BenefitBean b : benefits) {
            benefitSql.append("INSERT INTO dbo.benefit (public_id, benefit_name, is_member_only, benefit_type, status, "
                            + "start_date, end_date, official_image_url, source_url, review_note, created_at, updated_at) VALUES (")
                    .append(str(b.getPublicId())).append(", ")
                    .append(str(b.getBenefitName())).append(", ")
                    .append(Boolean.TRUE.equals(b.getIsMemberOnly()) ? 1 : 0).append(", ")
                    .append(str(b.getBenefitType())).append(", ")
                    .append(str(b.getStatus())).append(", ")
                    .append(date(b.getStartDate())).append(", ")
                    .append(date(b.getEndDate())).append(", ")
                    .append(str(b.getOfficialImageUrl())).append(", ")
                    .append(str(b.getSourceUrl())).append(", ")
                    .append(str(b.getReviewNote())).append(", SYSDATETIME(), SYSDATETIME());\n");
        }

        // 同一個特典＋同一部電影合成一句SQL，影城用IN (...)列出
        Map<String, List<CinemaMovieBenefitBean>> groups = new LinkedHashMap<>();
        links.stream()
                .sorted(Comparator.comparing((CinemaMovieBenefitBean l) -> l.getBenefit().getBenefitId())
                        .thenComparing(l -> l.getMovie().getMovieId())
                        .thenComparing(l -> l.getCinema().getCinemaId()))
                .forEach(l -> groups.computeIfAbsent(
                        l.getBenefit().getBenefitId() + "|" + l.getMovie().getMovieId(), k -> new ArrayList<>()).add(l));

        for (List<CinemaMovieBenefitBean> g : groups.values()) {
            BenefitBean b = g.get(0).getBenefit();
            String movieName = g.get(0).getMovie().getMovieName();
            String cinemas = g.stream().map(l -> l.getCinema().getCinemaName()).distinct()
                    .map(BenefitSeedSqlGenerator::str).collect(Collectors.joining(", "));

            linkSql.append("INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)\n")
                    .append("SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()\n")
                    .append("FROM dbo.cinema c, dbo.movie m, dbo.benefit b\n")
                    .append("WHERE c.cinema_name IN (").append(cinemas).append(")\n")
                    .append("  AND m.movie_name = ").append(str(movieName)).append("\n")
                    .append("  AND b.benefit_name = ").append(str(b.getBenefitName()))
                    .append(" AND ").append(equalsOrNull("b.source_url", b.getSourceUrl()))
                    .append(" AND ").append(equalsOrNull("b.start_date", b.getStartDate() == null ? null : DT.format(b.getStartDate())))
                    .append(";\n");
        }
        return new SqlFiles(benefitSql.toString(), linkSql.toString());
    }

    private static String equalsOrNull(String column, String value) {
        return value == null ? column + " IS NULL" : column + " = " + str(value);
    }

    /** 字串轉成SQL Server的 N'...'，單引號要重複一次 */
    static String str(String s) {
        return s == null ? "NULL" : "N'" + s.replace("'", "''") + "'";
    }

    private static String date(LocalDateTime d) {
        return d == null ? "NULL" : "'" + d.format(DT) + "'";
    }
}