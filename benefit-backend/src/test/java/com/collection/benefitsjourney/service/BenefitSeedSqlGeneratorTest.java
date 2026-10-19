package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.service.BenefitSeedSqlGenerator.SqlFiles;

class BenefitSeedSqlGeneratorTest {

    private BenefitBean benefit(int id, String name, String url) {
        BenefitBean b = new BenefitBean();
        b.setBenefitId(id);
        b.setBenefitName(name);
        b.setBenefitType("REGULAR");
        b.setStatus("APPROVED");
        b.setIsMemberOnly(false);
        b.setStartDate(LocalDateTime.of(2026, 10, 16, 0, 0));
        b.setEndDate(LocalDateTime.of(2026, 10, 22, 23, 59, 59));
        b.setSourceUrl(url);
        return b;
    }

    private CinemaMovieBenefitBean link(BenefitBean b, int movieId, String movie, int cinemaId, String cinema) {
        MovieBean m = new MovieBean();
        m.setMovieId(movieId);
        m.setMovieName(movie);
        CinemaBean c = new CinemaBean();
        c.setCinemaId(cinemaId);
        c.setCinemaName(cinema);
        CinemaMovieBenefitBean l = new CinemaMovieBenefitBean();
        l.setBenefit(b);
        l.setMovie(m);
        l.setCinema(c);
        return l;
    }

    @Test
    @DisplayName("特典本體：單引號跳脫、空值寫NULL、不寫死ID")
    void benefitSql() {
        BenefitBean b = benefit(1, "Mujica's 海報", "https://fb/p1");
        SqlFiles f = BenefitSeedSqlGenerator.generate(List.of(b), List.of());

        assertTrue(f.benefitSql().contains("N'Mujica''s 海報'"));
        assertTrue(f.benefitSql().contains("'2026-10-16 00:00:00'"));
        assertTrue(f.benefitSql().contains("INSERT INTO dbo.benefit (public_id,"));
        assertTrue(f.benefitSql().contains("VALUES (NULL, N'Mujica''s 海報'"));
    }

    @Test
    @DisplayName("關聯：同特典＋同電影的多間影城合成一句，用名稱查 ID")
    void linkSql() {
        BenefitBean b = benefit(1, "A3海報", "https://fb/p1");
        SqlFiles f = BenefitSeedSqlGenerator.generate(List.of(b), List.of(
                link(b, 5, "惡靈古堡：爆發夜", 2, "台北信義威秀"),
                link(b, 5, "惡靈古堡：爆發夜", 3, "板橋大遠百威秀")));

        String sql = f.cinemaMovieBenefitSql();
        assertEquals(1, sql.split("INSERT INTO dbo.cinema_movie_benefit", -1).length - 1);
        assertTrue(sql.contains("c.cinema_name IN (N'台北信義威秀', N'板橋大遠百威秀')"));
        assertTrue(sql.contains("m.movie_name = N'惡靈古堡：爆發夜'"));
        assertTrue(sql.contains("b.source_url = N'https://fb/p1'"));
        assertTrue(sql.contains("b.start_date = N'2026-10-16 00:00:00'"));
    }

    @Test
    @DisplayName("沒有來源貼文的特典（手動新增）：source_url用IS NULL比對")
    void nullSourceUrl() {
        BenefitBean b = benefit(2, "手動特典", null);
        SqlFiles f = BenefitSeedSqlGenerator.generate(List.of(b), List.of(link(b, 5, "片", 2, "影城")));
        assertTrue(f.cinemaMovieBenefitSql().contains("b.source_url IS NULL"));
    }

    @Test
    @DisplayName("不同電影各自一句")
    void twoMovies() {
        BenefitBean b = benefit(3, "海報", "u");
        SqlFiles f = BenefitSeedSqlGenerator.generate(List.of(b), List.of(
                link(b, 5, "片A", 2, "影城甲"), link(b, 6, "片B", 2, "影城甲")));
        assertEquals(2, f.cinemaMovieBenefitSql().split("INSERT INTO dbo.cinema_movie_benefit", -1).length - 1);
    }
}