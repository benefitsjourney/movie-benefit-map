package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest
/** 只用內嵌的假貼文，不讀檔、不連網。驗證分類、名稱、類型、起訖日期。 */
class BenefitPostParserTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    
    @Autowired
    private BenefitPostParser parser;

    private ObjectNode post(String text, String time) {
        ObjectNode node = mapper.createObjectNode();
        node.put("pageName", "vieshow");
        node.put("url", "https://www.facebook.com/Vieshow/posts/1");
        node.put("text", text);
        if (time != null) node.put("time", time);
        return node;
    }

    private static final String T_1015 = "2026-10-15T02:00:00.000Z";

    private static final String RESIDENT_EVIL = "《惡靈古堡：爆發夜》第五週購票特典。關燈才現身！「厄夜之鼠」A3夜光海報。"
            + "凡購買10/16起《惡靈古堡：爆發夜》任一版本電影票1張，於10/16白天營業時間起至原購票影城，"
            + "即可兌換領取「厄夜之鼠」款A3夜光海報1張。";

    private static final String SPIDERMAN = "《蜘蛛人：重生日》第12週特典。「破曉前行」款A3海報。"
            + "凡購買10/16起任一版本電影票1張，於10/16白天營業時間起至原購票影城，"
            + "即可兌換領取「破曉前行」款A3海報1張。";

    @Test
    void weeklyBenefit_typeNameAndSevenDays_andZhiIsNotARange() {
        ParsedBenefitPost p = parser.parse(post(RESIDENT_EVIL, T_1015));

        assertEquals(ParsedBenefitPost.Classification.BENEFIT, p.classification());
        assertEquals("厄夜之鼠款A3夜光海報", p.benefitName());
        assertEquals("惡靈古堡：爆發夜", p.movieName());
        assertEquals("REGULAR", p.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0, 0), p.startDate());
        // 「起至原購票影城」的「至」不能當區間；一般場補滿七天 → 10/22
        assertEquals(LocalDateTime.of(2026, 10, 22, 23, 59, 59), p.endDate());
    }

    @Test
    void weeklyBenefit_spiderman() {
        ParsedBenefitPost p = parser.parse(post(SPIDERMAN, T_1015));

        assertEquals(ParsedBenefitPost.Classification.BENEFIT, p.classification());
        assertEquals("破曉前行款A3海報", p.benefitName());
        assertEquals("REGULAR", p.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0, 0), p.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 22, 23, 59, 59), p.endDate());
    }

    @Test
    void notesSection_doesNotOverrideWeeklyType() {
        // 威秀注意事項裡一定有「數量有限，送完為止」，不能讓它把週特典判成 SPECIAL_UNTIL_OUT
        String text = "《驀然回首》第3週特典。凡購買09/11-09/17期間任一場次電影票1張，即可兌換領取「A6劇照光柵卡」1張。\n"
                + "◢◤特典領取注意事項◢◤\n"
                + "❶贈品發放以現場兌換順序為主（無論場次先後），數量有限，送完為止。";
        ParsedBenefitPost p = parser.parse(post(text, "2026-09-08T10:00:00.000Z"));

        assertEquals("REGULAR", p.benefitType());
        assertEquals("A6劇照光柵卡", p.benefitName());
        assertEquals(LocalDateTime.of(2026, 9, 11, 0, 0, 0), p.startDate());
        assertEquals(LocalDateTime.of(2026, 9, 17, 23, 59, 59), p.endDate());
    }

    @Test
    void fullWidthDigits_areParsed() {
        String text = "《測試電影》第３週特典。凡購買９月１１日至９月１７日期間電影票１張，即可兌換領取「測試海報」１張。";
        ParsedBenefitPost p = parser.parse(post(text, "2026-09-08T10:00:00.000Z"));

        assertEquals("REGULAR", p.benefitType());
        assertEquals(LocalDateTime.of(2026, 9, 11, 0, 0, 0), p.startDate());
        assertEquals(LocalDateTime.of(2026, 9, 17, 23, 59, 59), p.endDate());
    }

    @Test
    void mathematicalDigits_andNoBenefitKeyword_showtimeStylePost() {
        // 秀泰風格：沒有「特典／贈品」字樣、數字是特殊字元、開賣日不能當領取日
        String text = "《再見機器人》美好定格．相遇紀念場\n"
                + "𝟮𝟬𝟮𝟲/𝟵/𝟭𝟱（二）𝟭𝟭:𝟬𝟬 開賣\n"
                + "📅活動日期：𝟵/𝟮𝟬（日）、𝟵/𝟮𝟭（一）\n"
                + "單人套票 $𝟰𝟱𝟬\n1 張 電影票\n1 組「定格瞬間：九月」A3 厚磅海報\n"
                + "⚠️注意事項\n● 數量有限，送完為止。";
        ParsedBenefitPost p = parser.parse(post(text, "2026-09-11T00:00:00.000Z"));

        assertEquals(ParsedBenefitPost.Classification.BENEFIT, p.classification());
        assertEquals("定格瞬間：九月", p.benefitName());
        assertEquals("再見機器人", p.movieName());
        assertEquals("SPECIAL_BY_DATE", p.benefitType());   // 「送完為止」在注意事項，不計入
        assertEquals(LocalDateTime.of(2026, 9, 20, 0, 0, 0), p.startDate()); // 不是 9/15 開賣日
        assertNull(p.endDate());                              // 9/20、9/21 用頓號分隔，不是區間
    }

    @Test
    void zhiWithoutRange_endDateStaysNull() {
        String text = "《特別場電影》特別場入場禮。凡購買10/16起電影票1張，於10/16起至原購票影城，即可兌換領取「限定貼紙」1張。";
        ParsedBenefitPost p = parser.parse(post(text, T_1015));

        assertEquals("SPECIAL_BY_DATE", p.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0, 0), p.startDate());
        assertNull(p.endDate());
    }

    @Test
    void onePostWithTwoBenefits_returnsTwoCandidates() {
        String text = "《名偵探柯南》4DX加強版9月11日狂飆上映！凡購買本片「4DX加強版」任一場次之電影票1張，"
                + "於09/11(五)白天營業時間起至原購票影城，即可兌換領取【 A3特典海報 】1張 ＋【 青山剛昌老師原畫劇照紀念卡組(1組3張) 】1組。\n"
                + "◢◤特典領取注意事項◢◤\n❶數量有限，送完為止。";
        List<ParsedBenefitPost> list = parser.parseAll(post(text, "2026-09-08T10:00:00.000Z"));

        assertEquals(2, list.size());
        assertEquals("A3特典海報", list.get(0).benefitName());
        assertEquals("青山剛昌老師原畫劇照紀念卡組(1組3張)", list.get(1).benefitName());
        assertEquals("https://www.facebook.com/Vieshow/posts/1", list.get(1).sourceUrl());
    }

    @Test
    void incompletePost_isMarkedIncomplete_notGuessed() {
        // 沒有日期、沒有類型字樣 → 不猜，交人工
        ParsedBenefitPost p = parser.parse(post(
                "活動期間，凡觀賞《Always Lalisa》不限版本之任一場次電影，即可於入場前領取「電影限定明信片」一張。", null));

        assertEquals(ParsedBenefitPost.Classification.INCOMPLETE, p.classification());
        assertEquals("電影限定明信片", p.benefitName());
        assertEquals("Always Lalisa", p.movieName());
        assertNull(p.startDate());
        assertNull(p.benefitType());
    }

    @Test
    void comments_areIgnored() {
        ObjectNode node = post(SPIDERMAN, T_1015);
        node.putArray("comments").addObject()
                .put("text", "特典名稱：假海報 10月1日至10月2日 送完為止");

        ParsedBenefitPost p = parser.parse(node);

        assertEquals("破曉前行款A3海報", p.benefitName());
        assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0, 0), p.startDate());
        assertEquals("REGULAR", p.benefitType());
    }

    @Test
    void notBenefit_normalPostAndMeetAndGreet() {
        assertEquals(ParsedBenefitPost.Classification.NOT_BENEFIT,
                parser.parse(post("導演將來到影城與影迷見面交流，映後活動結束後可憑票根簽名互動。", null)).classification());
        assertEquals(ParsedBenefitPost.Classification.NOT_BENEFIT,
                parser.parse(post("《哥吉拉-0.0》前導預告公開！11月6日上映。", null)).classification());
    }

    @Test
    void imagePost_fromNonVieshowPage_isNotSilentlyDropped() {
        ObjectNode node = mapper.createObjectNode();
        node.put("pageName", "showtimecinema");
        node.put("url", "https://www.facebook.com/showtimecinema/posts/9");
        node.put("text", "《再見機器人》相遇紀念場開賣情報公開！");
        node.putArray("media").addObject()
                .put("__typename", "Photo")
                .putObject("image").put("uri", "https://cdn.example.com/a.jpg");

        assertEquals(ParsedBenefitPost.Classification.INCOMPLETE, parser.parse(node).classification());

        node.put("text", "晚安");
        assertEquals(ParsedBenefitPost.Classification.NOT_BENEFIT, parser.parse(node).classification());
    }
}
