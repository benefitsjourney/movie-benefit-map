package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.sourcerules.VieshowPostRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 用威秀真實貼文測試 Parser（一般週特典） */
class BenefitPostParserTest {

    private static final BenefitPostParser parser = new BenefitPostParser(List.of(new VieshowPostRules()));
    private static JsonNode posts;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = BenefitPostParserTest.class.getResourceAsStream("/fixtures/vieshow30.json")) {
            posts = new ObjectMapper().readTree(in);
        }
    }

    private static List<ParsedBenefitPost> parse(String snippet) {
        for (JsonNode p : posts) {
            if (p.get("text").asText().contains(snippet)) {
                return parser.parseAll(p.get("postId").asText(), p.get("facebookUrl").asText(),
                        p.get("url").asText(), p.get("pageName").asText(), p.get("text").asText(),
                        Instant.parse(p.get("time").asText()));
            }
        }
        throw new IllegalStateException("找不到貼文：" + snippet);
    }

    @Test
    @DisplayName("盜作：有起訖日期")
    void yorushika() {
        ParsedBenefitPost r = parse("盜作主題壓克力吊飾").get(0);
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("Yorushika LIVE「盜作」", r.movieName());
        assertEquals("盜作主題壓克力吊飾", r.benefitName());
        assertEquals("REGULAR", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 22, 23, 59, 59), r.endDate());
        assertEquals(Boolean.FALSE, r.memberOnly());
        assertEquals("威秀", r.chainKeyword());
        assertEquals(LocalDateTime.of(2026, 10, 14, 19, 0, 51), r.publishedAt()); // UTC 11:00 → 台北 19:00
    }

    @Test
    @DisplayName("惡靈古堡：只有起日，結束日 = 起日 + 6 天")
    void residentEvil() {
        ParsedBenefitPost r = parse("厄夜之鼠").get(0);
        assertEquals("惡靈古堡：爆發夜", r.movieName());
        assertEquals("厄夜之鼠款A3夜光海報", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 22, 23, 59, 59), r.endDate());
    }

    @Test
    @DisplayName("蜘蛛人")
    void spiderMan() {
        ParsedBenefitPost r = parse("破曉前行").get(0);
        assertEquals("蜘蛛人：重生日", r.movieName());
        assertEquals("破曉前行款A3海報", r.benefitName());
    }

    @Test
    @DisplayName("泥面人特別場：贈品合成一筆、放映日期當起訖、影城來自「放映影城」")
    void specialByDate() {
        ParsedBenefitPost r = parse("面對恐懼A3工藝海報 x1").get(0);
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("SPECIAL_BY_DATE", r.benefitType());
        assertEquals("泥面人", r.movieName());
        assertEquals("特別場套組：面對恐懼A3工藝海報、原文漫畫、4DX限定A3海報、主視覺A3海報", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 10, 24, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 25, 23, 59, 59), r.endDate());
        assertNull(r.chainKeyword());
        assertEquals(List.of("台北信義", "林口三井OUTLET", "台南大遠百", "高雄大遠百"), r.cinemaNames());
    }

    @Test
    @DisplayName("哥吉拉首映：單日、多行影城清單、括號備註不影響")
    void premiere() {
        ParsedBenefitPost r = parse("特殊工藝日本原版A3海報+神秘驚喜禮").get(0);
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("哥吉拉-0.0", r.movieName());
        assertEquals("首映限定特典—特殊工藝日本原版A3海報+神秘驚喜禮", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 11, 5, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 11, 5, 23, 59, 59), r.endDate());
        assertTrue(r.cinemaNames().contains("台北信義"));
        assertTrue(r.cinemaNames().contains("台中老虎城"));      // TIGER CITY 別名
        assertTrue(r.cinemaNames().contains("台中大遠百"));      // 括號備註已移除
        assertEquals(22, r.cinemaNames().size());
    }

    @Test
    @DisplayName("快打旋風：限量兌換型只有開始日")
    void limitedRedeem() {
        ParsedBenefitPost r = parse("BLANKA 娃娃購物袋").get(0);
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("SPECIAL_UNTIL_OUT", r.benefitType());
        assertEquals("快打旋風", r.movieName());
        assertEquals("STREET FIGHTER 6 BLANKA 娃娃購物袋", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0), r.startDate());
        assertNull(r.endDate());
        assertEquals(List.of("台北南港LaLaport威秀影城", "台北京站威秀影城"), r.cinemaNames());
    }

    @Test
    @DisplayName("我們不是什麼：電影名在【】、沒有放映日期 → INCOMPLETE")
    void bracketMovieNoDate() {
        ParsedBenefitPost r = parse("全球唯一的素描版海報").get(0);
        assertEquals(Classification.INCOMPLETE, r.classification());
        assertEquals("我們不是什麼", r.movieName());
        assertEquals("台灣專屬 全球唯一的素描版海報", r.benefitName());
        assertTrue(r.reviewNote().contains("活動日期"));
    }

    @Test
    @DisplayName("LISA明信片：沒有日期 → INCOMPLETE；Stray Kids 特典未公布 → INCOMPLETE")
    void incompleteCases() {
        assertEquals(Classification.INCOMPLETE, parse("電影限定明信片").get(0).classification());
        assertEquals(Classification.INCOMPLETE, parse("即將啟售").get(0).classification());
    }

    @Test
    @DisplayName("懶人包、更換公告、一般宣傳 → 空 list")
    void notBenefit() {
        assertTrue(parse("本週購票特典懶人包").isEmpty());
        assertTrue(parse("特典更換公告").isEmpty());
        assertTrue(parse("奧德賽爆米花桶").isEmpty());
    }

    @Test
    @DisplayName("沒有對應規則的粉專 → 空 list")
    void unknownChain() {
        assertTrue(parser.parseAll("1", "https://www.facebook.com/miramar", "u", "x",
                "凡購買10/16(五)起，白天營業時間起至原購票影城，即可兌換領取「A」1個", Instant.now()).isEmpty());
    }

    @Test
    @DisplayName("跨年：12月發文講1月的日期 → 隔年")
    void yearRollover() {
        String text = "《測試片》 凡購買12/30(二)~1/5(一)期間任一場次電影票1張，於12/30(二)白天營業時間起至原購票影城，即可兌換領取「新年杯」１個。\n特典領取注意事項";
        ParsedBenefitPost r = parser.parseAll("1", "https://www.facebook.com/vieshow", "u", "vieshow",
                text, Instant.parse("2026-12-28T04:00:00Z")).get(0);
        assertEquals(LocalDateTime.of(2026, 12, 30, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2027, 1, 5, 23, 59, 59), r.endDate());
    }
}
