package com.collection.benefitsjourney.service.sourcerules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;

/** 秀泰貼文分類（內容取自秀泰.json的真實貼文，只留判斷需要的段落） */
class ShowtimePostRulesTest {

    private final ShowtimePostRules rules = new ShowtimePostRules();

    @Test
    @DisplayName("只處理秀泰粉專")
    void supports() {
        assertTrue(rules.supports("https://www.facebook.com/showtimecinemas"));
        assertFalse(rules.supports("https://www.facebook.com/vieshow"));
        assertFalse(rules.supports(null));
    }

    @Nested
    @DisplayName("三種特典")
    class Benefits {
        @Test
        @DisplayName("週特典：#秀泰隨票贈，演唱會電影的週特典也算")
        void weekly() {
            String t = """
                    #秀泰隨票贈 •《#SB19WakasAtSimula》第二週觀影特典 .ᐟ.ᐟ
                    A’TIN，第二週也要繼續和 SB19 相聚💙
                    購買《SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas》𝟭𝟬/𝟭𝟲 起之指定版本、場次電影票 𝟭 張，即可獲得對應版本特典：
                    """;
            assertEquals(Classification.BENEFIT, rules.classifyPost(t + "演唱會"));
            assertEquals(Optional.of(BenefitType.REGULAR), rules.classifyBenefitType(t));
        }

        @Test
        @DisplayName("特別場：單人套票＋特典統一於入場時領取，附帶映前抽獎也照樣是特典")
        void special() {
            String t = """
                    《#快打旋風》特別場
                    ▌𝗦𝗖𝗥𝗘𝗘𝗡𝗫 昇龍拳特別場｜$𝟲𝟬𝟬
                    1 張【獨家】SCREENX 海報
                    🎁映前加碼抽獎
                    ● 特典統一於入場時憑實體票及特典兌換券領取，僅限活動當日兌換。
                    """;
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.SPECIAL_BY_DATE), rules.classifyBenefitType(t));
        }

        @Test
        @DisplayName("首映套票：沒有「特典統一於入場時」，但有套票價格和首映")
        void premierePackage() {
            String t = """
                    《哥吉拉-0.0》全台聯合首映套票正式解禁🔥
                    🎬 《哥吉拉-0.0》全台聯合首映套票 $𝟭,𝟭𝟴𝟬
                    ▸ 首映限定特典 ×1
                    """;
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.SPECIAL_BY_DATE), rules.classifyBenefitType(t));
        }

        @Test
        @DisplayName("預售禮：#秀泰預售＋即送【…】")
        void presale() {
            String t = """
                    #秀泰預售．《#挖掘者》
                    ✨活動辦法：開放購票起，只要購買《挖掘者》預售電影票 𝟏 張，即送【限量 A3 電影海報】𝟏 張
                    """;
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.SPECIAL_UNTIL_OUT), rules.classifyBenefitType(t));
        }
    }

    @Nested
    @DisplayName("略過與送審核")
    class NotBenefit {
        @Test
        @DisplayName("本週隨票贈整理（往後滑）、更換公告、發放調整公告")
        void roundupAndNotice() {
            for (String t : List.of(
                    "#秀泰隨票贈\n本週隨票贈整理來啦 .ᐟ.ᐟ\n快往後滑查看各電影特典的活動日期👉",
                    "【航海王映畫祭｜《強者天下》隨票贈海報更換公告】",
                    "【活動特典發放調整公告】\n⏰補領期間\n𝟭𝟬/𝟮（五）－𝟭𝟬/𝟭𝟲（五）")) {
                assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(t), t);
            }
        }

        @Test
        @DisplayName("NBA轉播、直播：就算有「特典統一於入場時」也略過")
        void live() {
            String t = "在家看球哪夠嗨，球迷一起衝秀泰⛹️\nNBA轉播套票 $500\n● 特典統一於入場時憑實體票領取";
            assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(t));
        }

        @Test
        @DisplayName("影人見面場、抽獎、套餐")
        void noise() {
            for (String t : List.of(
                    "《#鼠一般的你》影人見面場\n1 張 電影票\n1 張《鼠一般的你》A3 正式海報",
                    "#秀泰抽獎 •《#誤闖遺忘島》",
                    "#秀泰獨家\n《哥吉拉-0.0》獨家電影套餐 $𝟳𝟲𝟱\n中份爆米花 ×1")) {
                assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(t), t);
            }
        }

        @Test
        @DisplayName("開箱文：有開箱、沒有活動日期→略過")
        void openBox() {
            assertEquals(Classification.NOT_BENEFIT, rules.classifyPost("〈號角日報〉光柵A3海報開箱來了🤎"));
        }

        @Test
        @DisplayName("「活動特典｜「…」」但不是三種特典→送審核")
        void otherGift() {
            String t = "《#我在學校旅行中被分到了一個糟糕的小組》\n📅 活動日期｜𝟭𝟬/𝟭𝟭 （日）\n💌 活動特典｜「紀念透明小卡」乙張";
            assertEquals(Classification.INCOMPLETE, rules.classifyPost(t));
            assertTrue(rules.incompleteNote(t).contains("請人工確認"));
        }
    }

    @Nested
    @DisplayName("特殊廳影城")
    class Halls {
        @Test
        @DisplayName("寫了版本：只綁有該廳的影城")
        void halls() {
            assertEquals(List.of("大巨蛋", "文心"), rules.versionCinemas("購買《泥面人》ULTRA 4DX 版本電影票 1 張", ""));
            assertEquals(8, rules.versionCinemas("購買《泥面人》SCREENX 版本電影票 1 張", "").size());
            assertEquals(List.of("大巨蛋", "站前"), rules.versionCinemas("Dolby Vision + Atmos 版本", ""));
            assertEquals(8, rules.versionCinemas("INFINITY VISION 版本電影票 1 張", "").size());
        }

        @Test
        @DisplayName("不限版本、任一版本、一般版本、其他版本、2D：全台秀泰")
        void anyVersion() {
            assertNull(rules.versionCinemas("購買《蜘蛛人：重生日》10/2（五）起任一場次電影票 1 張（不限版本）", ""));
            assertNull(rules.versionCinemas("購買《快打旋風》一般版本電影票 1 張", ""));
            assertNull(rules.versionCinemas("購買 其他版本電影票 1 張", ""));
            assertNull(rules.versionCinemas("2D 版本", ""));
        }

        @Test
        @DisplayName("特殊版本：貼文有寫是哪幾種就照寫的，沒寫就用ULTRA 4DX、SCREENX、杜比三種（8間）")
        void specialVersion() {
            assertEquals(8, rules.versionCinemas("特殊版本", "").size());
            assertEquals(List.of("大巨蛋", "文心"),
                    rules.versionCinemas("特殊版本任一場次電影票", "※ 特殊版本為 Ultra4DX\n"));
        }

        @Test
        @DisplayName("從一段文字找影城簡稱，依出現順序")
        void extractCinemaNames() {
            assertEquals(List.of("大巨蛋", "文心"), rules.extractCinemaNames("📍活動影城：大巨蛋、文心"));
            assertEquals(List.of("站前", "大巨蛋"), rules.extractCinemaNames("𝟭𝟬/𝟮𝟭（三）｜站前、大巨蛋秀泰影城"));
            assertEquals(List.of("欣欣"), rules.extractCinemaNames("台北欣欣秀泰影城"));
        }
    }
}
