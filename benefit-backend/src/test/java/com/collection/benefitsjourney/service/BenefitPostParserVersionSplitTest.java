package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.sourcerules.VieshowPostRules;

/** 週特典寫「2D、IMAX場次…即可獲得相對應版本之特典」時，一個版本拆一筆 */
class BenefitPostParserVersionSplitTest {

    private static final BenefitPostParser parser = new BenefitPostParser(List.of(new VieshowPostRules()));
    private static final String PAGE = "https://www.facebook.com/vieshow";
    private static final String TIME = "2026-10-27T02:00:00Z";

    private static List<ParsedBenefitPost> parse(String text) {
        return parser.parseAll("id", PAGE, PAGE + "/posts/x", "vieshow", text, Instant.parse(TIME));
    }

    /** 哥吉拉-0.0首週預售貼文原文 */
    private static final String GODZILLA = """
───❱ 《#哥吉拉 -𝟬.𝟬》首周 預售情報 ───❱
𝟭𝟬/𝟮𝟴(三) 𝟭𝟮:𝟬𝟬起，開放 𝟭𝟭/𝟬𝟲~𝟭𝟭/𝟭𝟮 之𝟮𝗗、𝗜𝗠𝗔𝗫 場次
首周特典情報
凡購買𝟏𝟏/𝟎𝟔－𝟏𝟏/𝟏𝟐之《哥吉拉 -𝟬.𝟬》𝟮𝗗、𝗜𝗠𝗔𝗫 場次電影票 𝟏 張，即可獲得首週相對應版本之入場特典乙份。
𝟏𝟏/𝟎𝟔(五)開放領取，數量有限，送完為止。
特別場情報，敬請期待
【注意事項】
・預購完成後，無論場次先後，即可於 11/06 起，憑實體票或電子票至原購票影城領取，不得跨影城領取。
・每張票限兌特典乙份，每人每次限兌 4 張。數量有限，送完為止。
・團劃、包廳、免費票種(包含忠誠禮、兌點票)均不適用特典活動。
・如有退票，需連同特典一併辦理退票手續，如特典毀損恕無法辦理退票。
・圖片僅供參考，請以實物為準。
・威秀影城/MUVIE CINEMAS保有活動最終解釋權。
""";

    @Test
    @DisplayName("哥吉拉：2D和IMAX各一筆，2D全台、IMAX只有IMAX廳，沒寫特典名稱用暫用名稱送審核")
    void godzilla() {
        List<ParsedBenefitPost> r = parse(GODZILLA);
        assertEquals(2, r.size());

        ParsedBenefitPost twoD = r.get(0);
        assertEquals("首週入場特典（2D）", twoD.benefitName());
        assertEquals("REGULAR", twoD.benefitType());
        assertEquals("威秀", twoD.chainKeyword());
        assertTrue(twoD.cinemaNames().isEmpty());
        assertEquals(LocalDateTime.of(2026, 11, 6, 0, 0), twoD.startDate());
        assertEquals(LocalDateTime.of(2026, 11, 12, 23, 59, 59), twoD.endDate());
        assertEquals(Classification.INCOMPLETE, twoD.classification());

        ParsedBenefitPost imax = r.get(1);
        assertEquals("首週入場特典（IMAX）", imax.benefitName());
        assertNull(imax.chainKeyword());
        assertEquals(7, imax.cinemaNames().size());
        assertTrue(imax.cinemaNames().contains("板橋大遠百"));
        assertEquals(LocalDateTime.of(2026, 11, 12, 23, 59, 59), imax.endDate());
    }

    @Test
    @DisplayName("2D、IMAX、4DX三個版本而且有寫特典名稱：拆三筆，資料完整可以自動上架")
    void threeFormatsWithName() {
        String text = GODZILLA
                .replace("𝟮𝗗、𝗜𝗠𝗔𝗫 場次電影票", "2D、IMAX、4DX 場次電影票")
                .replace("之入場特典乙份", "「哥吉拉怪獸 A3海報」1張");
        List<ParsedBenefitPost> r = parse(text);
        assertEquals(3, r.size());
        assertEquals("哥吉拉怪獸 A3海報（2D）", r.get(0).benefitName());
        assertEquals("哥吉拉怪獸 A3海報（IMAX）", r.get(1).benefitName());
        assertEquals("哥吉拉怪獸 A3海報（4DX）", r.get(2).benefitName());
        assertEquals(4, r.get(2).cinemaNames().size());
        assertEquals(Classification.BENEFIT, r.get(0).classification());
    }

    @Test
    @DisplayName("沒寫週次、只寫「A起」：名稱不加週次，2D算7天，IMAX送完為止")
    void noWeekStartOnly() {
        String text = """
                凡購買11/20(五)起《測試電影》2D、IMAX場次電影票1張，即可獲得相對應版本特典乙份。
                憑票至原購票影城領取，數量有限，送完為止。
                """;
        List<ParsedBenefitPost> r = parse(text);
        assertEquals(2, r.size());
        assertEquals("特典（2D）", r.get(0).benefitName());
        assertEquals("REGULAR", r.get(0).benefitType());
        assertEquals(LocalDateTime.of(2026, 11, 26, 23, 59, 59), r.get(0).endDate());
        assertEquals("特典（IMAX）", r.get(1).benefitName());
        assertEquals("SPECIAL_UNTIL_OUT", r.get(1).benefitType());
        assertNull(r.get(1).endDate());
    }

    @Test
    @DisplayName("只寫一個版本：不拆，照原本的流程")
    void singleFormatNotSplit() {
        String text = GODZILLA.replace("𝟮𝗗、𝗜𝗠𝗔𝗫 場次電影票", "2D 場次電影票");
        assertEquals(1, parse(text).size());
    }
}
