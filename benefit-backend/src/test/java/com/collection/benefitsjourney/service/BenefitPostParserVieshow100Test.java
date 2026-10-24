package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.sourcerules.VieshowPostRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 用100篇威秀貼文（fixtures/vieshow100.json）測試：分版本特典、疑似特典、排除項目 */
class BenefitPostParserVieshow100Test {

    private static final BenefitPostParser parser = new BenefitPostParser(List.of(new VieshowPostRules()));
    private static JsonNode posts;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = BenefitPostParserVieshow100Test.class.getResourceAsStream("/fixtures/vieshow100.json")) {
            posts = new ObjectMapper().readTree(in);
        }
    }

    private static List<ParsedBenefitPost> parseOne(JsonNode p) {
        return parser.parseAll(p.get("postId").asText(), p.get("facebookUrl").asText(),
                p.get("url").asText(), p.get("pageName").asText(), p.get("text").asText(),
                Instant.parse(p.get("time").asText()));
    }

    /** 用貼文裡的一段字找第一篇符合的貼文 */
    private static List<ParsedBenefitPost> parse(String snippet) {
        for (JsonNode p : posts) {
            if (p.get("text").asText().contains(snippet)) {
                return parseOne(p);
            }
        }
        throw new IllegalStateException("找不到貼文：" + snippet);
    }

    @Test
    @DisplayName("100篇總數：25筆資料齊全、3筆待人工確認、73篇略過")
    void totals() {
        int benefit = 0;
        int incomplete = 0;
        int ignoredPosts = 0;
        for (JsonNode p : posts) {
            List<ParsedBenefitPost> r = parseOne(p);
            if (r.isEmpty()) {
                ignoredPosts++;
            }
            for (ParsedBenefitPost x : r) {
                if (x.classification() == Classification.BENEFIT) {
                    benefit++;
                } else {
                    incomplete++;
                }
            }
        }
        assertEquals(25, benefit);
        assertEquals(3, incomplete);
        assertEquals(73, ignoredPosts);
    }

    @Test
    @DisplayName("吉伊卡哇4DX加印海報：特典在【】裡、起日取「白天營業時間起」而不是票根日期")
    void chiikawa4dx() {
        ParsedBenefitPost r = parse("加碼加印的【4DX限定").get(0);
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("4DX限定｜A3主視覺海報", r.benefitName());
        assertEquals("SPECIAL_UNTIL_OUT", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 7, 0, 0), r.startDate());
        assertEquals(4, r.cinemaNames().size());
    }

    @Test
    @DisplayName("角落小夥伴貼紙：「凡購買上映期間」＝整檔期、全連鎖影城，起日取上映日")
    void cornerGurashi() {
        ParsedBenefitPost r = parse("電影造型貼紙").get(0);
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("SPECIAL_UNTIL_OUT", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 8, 0, 0), r.startDate());
        assertEquals("威秀", r.chainKeyword());
    }

    @Test
    @DisplayName("蜘蛛人雙十特別場：特典在【#獨家】、「雙十」＝10/10")
    void spiderManDoubleTen() {
        ParsedBenefitPost r = parse("網力全開A3夜光海報").get(0);
        assertEquals("網力全開A3夜光海報", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 10, 10, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 10, 23, 59, 59), r.endDate());
    }

    @Test
    @DisplayName("遊戲人生第一週特典開箱：只有開箱、沒有活動日期→略過，內容在另一篇售票貼文")
    void yugioh() {
        assertTrue(parse("棋局降臨").isEmpty());
    }

    @Test
    @DisplayName("復仇者終局之戰：4DX／IMAX 各一款特典，拆成兩筆，影城按照廳別")
    void avengersByVersion() {
        List<ParsedBenefitPost> r = parse("4DX特製 A3海報");
        assertEquals(2, r.size());
        assertEquals("4DX特製 A3海報", r.get(0).benefitName());
        assertEquals(4, r.get(0).cinemaNames().size());
        assertEquals("IMAX特製 A3海報", r.get(1).benefitName());
        assertEquals(7, r.get(1).cinemaNames().size());
        assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), r.get(0).startDate());
    }

    @Test
    @DisplayName("快打旋風4DX特別場預購：格式太雜，但不能直接丟掉→轉人工；預購粉絲禮、角色海報組只是開箱文→略過")
    void streetFighterSuspects() {
        List<ParsedBenefitPost> r = parse("影迷限定４ＤＸ珍藏海報");
        assertTrue(!r.isEmpty());
        assertNotEquals(Classification.BENEFIT, r.get(0).classification());
        assertTrue(parse("預購粉絲禮").isEmpty());
        assertTrue(parse("角色A3海報組(共3款)").isEmpty());
    }

    @Test
    @DisplayName("不該收的：芬達套餐贈品、本週特典開箱、特典更換公告")
    void excluded() {
        assertTrue(parse("芬達").isEmpty());
        assertTrue(parse("📦 本週特典開箱").isEmpty());
        assertTrue(parse("第四彈特典").isEmpty());
    }

    @Test
    @DisplayName("轉人工的備註要說清楚缺什麼")
    void suspectNote() {
        ParsedBenefitPost r = parse("影迷限定４ＤＸ珍藏海報").get(0);
        assertTrue(r.reviewNote().contains("特典名稱"));
    }

    @Test
    @DisplayName("每筆解析結果都要有貼文網址（去重用）")
    void everyResultHasUrl() {
        List<ParsedBenefitPost> all = new ArrayList<>();
        for (JsonNode p : posts) {
            all.addAll(parseOne(p));
        }
        assertTrue(all.stream().allMatch(x -> x.sourceUrl() != null && !x.sourceUrl().isBlank()));
    }
}

