package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** 用威秀真實貼文測試Parser（一般週特典） */
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
    @DisplayName("特別場：先標 INCOMPLETE 交給人工")
    void special() {
        ParsedBenefitPost r = parse("面對恐懼A3工藝海報 x1").get(0);
        assertEquals(Classification.INCOMPLETE, r.classification());
        assertEquals("SPECIAL_BY_DATE", r.benefitType());
        assertTrue(r.reviewNote().contains("特別場"));
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
