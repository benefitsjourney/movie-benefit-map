package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.sourcerules.ShowtimePostRules;
import com.collection.benefitsjourney.service.sourcerules.VieshowPostRules;

/** 用秀泰.json的真實貼文（只留解析需要的段落）測一篇拆多筆、日期、影城 */
class BenefitPostParserShowtimeTest {

    private static final BenefitPostParser parser = new BenefitPostParser(
            List.of(new VieshowPostRules(), new ShowtimePostRules()));

    /** time是發文時間（UTC），台北時間會加8小時 */
    private static List<ParsedBenefitPost> parse(String text, String time) {
        return parser.parseAll("1", "https://www.facebook.com/showtimecinemas",
                "https://www.facebook.com/showtimecinemas/posts/1", "showtimecinemas", text, Instant.parse(time));
    }

    private static LocalDateTime day(int month, int day) {
        return LocalDateTime.of(2026, month, day, 0, 0);
    }

    private static LocalDateTime endOf(int month, int day) {
        return LocalDateTime.of(2026, month, day, 23, 59, 59);
    }

    @Nested
    @DisplayName("週特典")
    class Weekly {
        @Test
        @DisplayName("A－B期間：照寫的起訖，全台秀泰")
        void range() {
            List<ParsedBenefitPost> r = parse("""
                    #秀泰隨票贈 •《#快打旋風》第二週觀影特典 .ᐟ.ᐟ
                    🥊活動辦法：
                    購買《快打旋風》𝟭𝟬/𝟮𝟯－𝟭𝟬/𝟮𝟵 期間之任一版本電影票 𝟭 張，即可獲得限量「電玩概念款 A3 海報」𝟭 張！
                    """, "2026-10-19T05:45:00Z");
            assertEquals(1, r.size());
            assertEquals("電玩概念款 A3 海報", r.get(0).benefitName());
            assertEquals("REGULAR", r.get(0).benefitType());
            assertEquals(day(10, 23), r.get(0).startDate());
            assertEquals(endOf(10, 29), r.get(0).endDate());
            assertEquals("秀泰", r.get(0).chainKeyword());
        }

        @Test
        @DisplayName("A起＋第N週：算7天。①②條列各一筆")
        void numberedItems() {
            List<ParsedBenefitPost> r = parse("""
                    #秀泰隨票贈 •《#蜘蛛人重生日》第十一週觀影特典 .ᐟ.ᐟ
                    購買《蜘蛛人：重生日》𝟭𝟬/𝟴 起之任一版本、場次電影票 𝟭 張，即可獲得：
                    ①「我是我 A3 海報」𝟭 張
                    ②「紅藍鏡片組」𝟭 組👓
                    ※ 𝟭𝟬/𝟴 起開放領取。
                    """, "2026-10-05T06:00:00Z");
            assertEquals(2, r.size());
            assertEquals("我是我 A3 海報", r.get(0).benefitName());
            assertEquals("紅藍鏡片組", r.get(1).benefitName());
            assertEquals(day(10, 8), r.get(1).startDate());
            assertEquals(endOf(10, 14), r.get(1).endDate());
        }

        @Test
        @DisplayName("追加印製的A起：不是每週輪替，送完為止")
        void additionalPrint() {
            ParsedBenefitPost r = parse("""
                    #秀泰隨票贈 •《#惡靈古堡爆發夜》
                    🩸好評追加印製．𝟭𝟬/𝟮 起開放兌換🩸
                    凡購買 𝟭𝟬/𝟮（五）起《惡靈古堡：爆發夜》電影票 𝟭 張（不限版本），即可獲得【暗夜之路】款 𝗔𝟯 海報 𝟭 張。
                    """, "2026-09-30T10:00:00Z").get(0);
            assertEquals("暗夜之路款 A3 海報", r.benefitName());
            assertEquals("SPECIAL_UNTIL_OUT", r.benefitType());
            assertEquals(day(10, 2), r.startDate());
            assertNull(r.endDate());
        }

        @Test
        @DisplayName("依版本送不同東西：一個版本一筆，4DX加贈的SCREENX版海報併進SCREENX那筆")
        void versions() {
            List<ParsedBenefitPost> r = parse("""
                    #秀泰隨票贈 •《#SB19_WakasAtSimula》首週觀影特典 .ᐟ.ᐟ
                    購買《SB19 Wakas at Simula》𝟭𝟬/𝟳－𝟭𝟬/𝟭𝟱 期間之指定版本、場次電影票 𝟭 張，即可獲得對應版本 A3 海報：
                    ▸ 2D 版本：2D 版海報 𝟭 張
                    ▸ SCREENX 版本：SCREENX 版海報 𝟭 張
                    ▸ ULTRA 4DX 版本：4DX 版海報 𝟭 張＋加贈 SCREENX 版海報 𝟭 張
                    """, "2026-10-05T08:01:00Z");
            assertEquals(3, r.size());
            assertEquals("2D 版海報", r.get(0).benefitName());
            assertEquals("秀泰", r.get(0).chainKeyword());
            assertEquals("SCREENX 版海報", r.get(1).benefitName());
            assertEquals(8, r.get(1).cinemaNames().size());
            assertEquals("4DX 版海報", r.get(2).benefitName());
            assertEquals(List.of("大巨蛋", "文心"), r.get(2).cinemaNames());
        }

        @Test
        @DisplayName("特殊版本：名稱後面加「（限特殊版本）」，影城是三種特殊廳合起來")
        void specialVersion() {
            List<ParsedBenefitPost> r = parse("""
                    #秀泰隨票贈 •《#SB19WakasAtSimula》第二週觀影特典 .ᐟ.ᐟ
                    購買《SB19 Wakas at Simula》𝟭𝟬/𝟭𝟲 起之指定版本、場次電影票 𝟭 張，即可獲得對應版本特典：
                    ▸ 特殊版本｜「獨照小卡」𝟭 組
                    ▸ 2D 版本｜「團體小卡」𝟭 張
                    """, "2026-10-17T07:00:00Z");
            assertEquals("獨照小卡（限特殊版本）", r.get(0).benefitName());
            assertEquals(8, r.get(0).cinemaNames().size());
            assertEquals(endOf(10, 22), r.get(0).endDate());
            assertEquals("團體小卡", r.get(1).benefitName());
        }

        @Test
        @DisplayName("對應版本兌換券＋▸版本清單：拆4筆，備註寫海報兌換期間")
        void godzilla() {
            List<ParsedBenefitPost> r = parse("""
                    #秀泰隨票贈 •《哥吉拉-1.0》經典限量重映特典 .ᐟ.ᐟ
                    ▸ 𝟮𝗗｜局部光
                    ▸ 𝗦𝗖𝗥𝗘𝗘𝗡𝗫｜亮銀龍
                    ▸ 𝗨𝗟𝗧𝗥𝗔 𝟰𝗗𝗫｜米色映畫 × 隱形油墨 × 磨砂
                    ▸ 𝗗𝗼𝗹𝗯𝘆 𝗩𝗶𝘀𝗶𝗼𝗻＋𝗔𝘁𝗺𝗼𝘀｜鑽卡 × 霧面排斥油
                    購買《哥吉拉-1.0》𝟭𝟬/𝟮𝟯（五）－𝟭𝟭/𝟬𝟮（一）期間之電影票 𝟭 張，即可領取相對應版本「A3 海報特典兌換券」𝟭 張。
                    📅預計海報兌換期間｜𝟭𝟭/𝟭𝟯－𝟭𝟭/𝟯𝟬
                    """, "2026-10-22T04:18:00Z");
            assertEquals(4, r.size());
            assertEquals("A3 海報特典兌換券（2D）", r.get(0).benefitName());
            assertEquals("秀泰", r.get(0).chainKeyword());
            assertEquals("A3 海報特典兌換券（ULTRA 4DX）", r.get(2).benefitName());
            assertEquals(List.of("大巨蛋", "文心"), r.get(2).cinemaNames());
            assertEquals(endOf(11, 2), r.get(3).endDate());
            assertTrue(r.get(0).reviewNote().contains("海報兌換期間11/13～11/30"));
        }

        @Test
        @DisplayName("同一篇不同領取期間：一個期間一筆")
        void twoPeriods() {
            List<ParsedBenefitPost> r = parse("""
                    #秀泰隨票贈 •《#實境誘捕》首週＋A24 電影紀念票 .ᐟ.ᐟ
                    🎬第一週觀影特典｜𝟵/𝟮𝟰－𝟭𝟬/𝟭
                    購買《實境誘捕》期間之任一場次電影票 𝟭 張，即可獲得限量「主視覺原文 A3 海報」𝟭 張。
                    🎟️A24 電影紀念票｜𝟵/𝟮𝟰－𝟵/𝟮𝟳
                    購買《實境誘捕》期間之任一場次電影票 𝟭 張，即可獲得限量「A24 電影紀念票」𝟭 張。
                    """, "2026-09-24T01:39:00Z");
            assertEquals(2, r.size());
            assertEquals(endOf(10, 1), r.get(0).endDate());
            assertEquals("A24 電影紀念票", r.get(1).benefitName());
            assertEquals(endOf(9, 27), r.get(1).endDate());
        }

        @Test
        @DisplayName("領取時間和領取影城：以領取時間為準、只綁列出的影城")
        void pickupTimeAndCinemas() {
            ParsedBenefitPost r = parse("""
                    秀泰隨票贈 •《#劇場版吉伊卡哇人魚島的秘密》
                    凡購買 𝟵/𝟭𝟴 起《劇場版 吉伊卡哇 人魚島的秘密》𝟰𝗗𝗫 版本電影票 𝟭 張，即可獲得【𝟰𝗗𝗫 海報】𝟭 張。
                    📍領取影城：大巨蛋、文心
                    📅領取時間：𝟭𝟬/𝟳（三）-𝟭𝟭/𝟮𝟬（五）
                    """, "2026-09-21T04:57:00Z").get(0);
            assertEquals("4DX 海報", r.benefitName());
            assertEquals(day(10, 7), r.startDate());
            assertEquals(endOf(11, 20), r.endDate());
            assertEquals(List.of("大巨蛋", "文心"), r.cinemaNames());
        }

        @Test
        @DisplayName("僅限當日：開始、結束都是那一天")
        void onlyOneDay() {
            ParsedBenefitPost r = parse("""
                    #秀泰隨票贈 •《#泥面人》SCREENX 首日瘋 .ᐟ.ᐟ
                    𝟭𝟬/𝟮𝟮 上映首日購買《泥面人》SCREENX 版本電影票 𝟭 張，即可獲得限量「SCREENX 透卡」𝟭 張！
                    ※ 僅限 𝟭𝟬/𝟮𝟮 當日 SCREENX 版本場次。
                    """, "2026-10-21T10:20:00Z").get(0);
            assertEquals(day(10, 22), r.startDate());
            assertEquals(endOf(10, 22), r.endDate());
            assertEquals(8, r.cinemaNames().size());
        }

        @Test
        @DisplayName("特典名稱沒有括號：抓不到名稱，送審核")
        void noBracket() {
            ParsedBenefitPost r = parse("""
                    #秀泰隨票贈 •《#復仇者聯盟終局之戰》加碼重映
                    凡購買《復仇者聯盟：終局之戰 加碼重映》𝟭𝟬/𝟮 起任一指定特殊版本場次「全票或早場票」𝟭 張，即可至原購票影城兌換兩張限量海報：
                    該版本電影海報 𝟭 張
                    """, "2026-10-01T10:20:00Z").get(0);
            assertEquals(Classification.INCOMPLETE, r.classification());
            assertTrue(r.reviewNote().contains("特典名稱"));
        }
    }

    @Nested
    @DisplayName("預售禮")
    class Presale {
        @Test
        @DisplayName("期間標題分段，每段依版本再拆：復仇者聯盟拆成3筆")
        void segments() {
            List<ParsedBenefitPost> r = parse("""
                    #秀泰預售．《#復仇者聯盟：#末日崛起》
                    🎬開放購買：𝟏𝟐/𝟏𝟔－𝟏𝟐/𝟐𝟐 部分場次
                    ｜ 𝟎𝟗/𝟐𝟖－𝟏𝟎/𝟏𝟏 期間限定｜
                    購買《復仇者聯盟：末日崛起》𝗜𝗡𝗙𝗜𝗡𝗜𝗧𝗬 𝗩𝗜𝗦𝗜𝗢𝗡 版本電影票 𝟏 張
                    ✦ 即送【𝗜𝗡𝗙𝗜𝗡𝗜𝗧𝗬 𝗩𝗜𝗦𝗜𝗢𝗡 𝗗𝟮𝟯 專屬特典】𝟏 張
                    ｜ 𝟏𝟎/𝟏𝟐－𝟏𝟎/𝟐𝟓 期間限定｜
                    購買 𝗜𝗡𝗙𝗜𝗡𝗜𝗧𝗬 𝗩𝗜𝗦𝗜𝗢𝗡 版本電影票 𝟏 張
                    ✦ 即送【末日登場特典】𝟏 張
                    購買 其他版本電影票 𝟏 張
                    ✦ 即送【末日崛起 復仇者標誌特典】𝟏 張
                    """, "2026-09-28T10:00:00Z");
            assertEquals(3, r.size());
            assertEquals(endOf(10, 11), r.get(0).endDate());
            assertEquals("末日登場特典（限INFINITY VISION版本）", r.get(1).benefitName());
            assertEquals(8, r.get(1).cinemaNames().size());
            assertEquals(day(10, 12), r.get(2).startDate());
            assertEquals("秀泰", r.get(2).chainKeyword());
        }

        @Test
        @DisplayName("特典領取時間A起：開始日用領取時間，送完為止，不是「開放購買」的場次日期")
        void pickupFrom() {
            ParsedBenefitPost r = parse("""
                    #秀泰預售．《#挖掘者》
                    ‼️開放購買：𝟏𝟎/𝟎𝟏－𝟏𝟎/𝟎𝟒 部分場次
                    ✨活動辦法：開放購票起，只要購買《挖掘者》預售電影票 𝟏 張，即送【限量 A3 電影海報】𝟏 張
                    🎁特典領取時間：𝟏𝟎/𝟎𝟏（四）起🎁
                    """, "2026-09-19T04:00:00Z").get(0);
            assertEquals("限量 A3 電影海報", r.benefitName());
            assertEquals(day(10, 1), r.startDate());
            assertNull(r.endDate());
            assertEquals("SPECIAL_UNTIL_OUT", r.benefitType());
        }

        @Test
        @DisplayName("有列首波限定場影城：照列的，期間限定是REGULAR")
        void listedCinemas() {
            ParsedBenefitPost r = parse("""
                    #秀泰預售．《沙丘：第三部》
                    🎁首波預售限定好禮
                    凡於 𝟏𝟎/𝟐𝟎－𝟏𝟏/𝟎𝟐 期間，購買《沙丘：第三部》首波限定場電影票 𝟏 張，即可獲得【沙丘凝視 A3 特典海報】𝟏 張。
                    🎬首波限定場影城／版本
                    ✦ 大巨蛋秀泰｜𝐃𝐨𝐥𝐛𝐲 𝐕𝐢𝐬𝐢𝐨𝐧 + 𝐀𝐭𝐦𝐨𝐬
                    ✦ 站前秀泰｜𝐃𝐨𝐥𝐛𝐲 𝐕𝐢𝐬𝐢𝐨𝐧 + 𝐀𝐭𝐦𝐨𝐬
                    ✦ 文心秀泰｜𝟒𝐃𝐗
                    ✦ 夢時代秀泰｜寬巨幕
                    """, "2026-10-19T13:30:00Z").get(0);
            assertEquals("REGULAR", r.benefitType());
            assertEquals(endOf(11, 2), r.endDate());
            assertEquals(List.of("大巨蛋", "站前", "文心", "夢時代"), r.cinemaNames());
        }
    }

    @Nested
    @DisplayName("特別場")
    class Special {
        @Test
        @DisplayName("一個場次一筆套組，排除電影票、飲料、爆米花和當週特典，日期取同場次最早到最晚")
        void sessions() {
            List<ParsedBenefitPost> r = parse("""
                    《#快打旋風》特別場
                    🥊 𝟮𝟬𝟮𝟲/𝟭𝟬/𝟭𝟮（一）中午 𝟭𝟮:𝟬𝟬 開賣 🥊
                    ━━━━━━ 👊 ━━━━━━
                    ▌𝗨𝗟𝗧𝗥𝗔 𝟰𝗗𝗫 昇龍拳特別場｜$𝟳𝟱𝟬
                    1 張 4DX 海報
                    1 組【獨家】隆、肯、春麗角色海報組（全套 3 款）
                    1 杯 大杯飲料
                    📅活動日期：𝟭𝟬/𝟭𝟲（五）、𝟭𝟬/𝟭𝟳（六）
                    📍活動影城：大巨蛋、文心
                    ━━━━━━ 👊 ━━━━━━
                    ▌𝗗𝗼𝗹𝗯𝘆 𝗩𝗶𝘀𝗶𝗼𝗻＋𝗔𝘁𝗺𝗼𝘀 波動拳特別場｜$𝟲𝟮𝟬
                    1 張 Dolby Vision＋Atmos 電影票
                    1 張 隆 杜比版・亮銀龍海報
                    1 張【當週特典】主視覺款 A3 海報
                    📅活動日期及影城：
                    𝟭𝟬/𝟮𝟭（三）站前
                    𝟭𝟬/𝟮𝟮（四）、𝟭𝟬/𝟮𝟲（一）大巨蛋
                    ━━━━━━ 👊 ━━━━━━
                    ● 特典統一於入場時憑實體票及特典兌換券領取，僅限活動當日兌換。
                    """, "2026-10-08T07:20:00Z");
            assertEquals(2, r.size());
            assertEquals("特別場套組：4DX 海報、【獨家】隆、肯、春麗角色海報組(全套 3 款)", r.get(0).benefitName());
            assertEquals("SPECIAL_BY_DATE", r.get(0).benefitType());
            assertEquals(endOf(10, 17), r.get(0).endDate());
            assertEquals(List.of("大巨蛋", "文心"), r.get(0).cinemaNames());
            assertEquals("特別場套組：隆 杜比版・亮銀龍海報", r.get(1).benefitName());
            assertEquals(day(10, 21), r.get(1).startDate());
            assertEquals(endOf(10, 26), r.get(1).endDate());
            assertEquals(List.of("站前", "大巨蛋"), r.get(1).cinemaNames());
        }

        @Test
        @DisplayName("日期：影城寫法、年份寫法")
        void dateFormats() {
            ParsedBenefitPost r = parse("""
                    蜘蛛人登上《號角日報》頭版啦🗞️
                    《#蜘蛛人：#重生日》號角日報場
                    🎬單人套票 $𝟳𝟳𝟬
                    1 張 電影票
                    1 張【獨家】《號角日報》光柵海報
                    📅活動日期、影城：
                    𝟭𝟬/𝟭𝟵（一）：大巨蛋、欣欣
                    𝟭𝟬/𝟮𝟬（二）：大巨蛋、土城
                    ● 特典統一於入場時憑實體票及特典兌換券領取
                    """, "2026-10-12T07:04:00Z").get(0);
            assertEquals("號角日報", r.movieName());   // 電影比對時會依序試每個《》，匯入時才對到蜘蛛人
            assertEquals(day(10, 19), r.startDate());
            assertEquals(endOf(10, 20), r.endDate());
            assertEquals(List.of("大巨蛋", "欣欣", "土城"), r.cinemaNames());

            ParsedBenefitPost g = parse("""
                    《#哥吉拉-0.0》 全台聯合首映場
                    🎬單人套票 $𝟰𝟴𝟬
                    1 張 特殊工藝日本原版 A3 海報
                    🗓️活動日期：𝟮𝟬𝟮𝟲/𝟭𝟭/𝟱（四）
                    📍活動影城：大巨蛋、文心、夢時代
                    ● 特典統一於入場時憑實體票及特典兌換券領取
                    """, "2026-10-13T04:00:00Z").get(0);
            assertEquals(day(11, 5), g.startDate());
        }

        @Test
        @DisplayName("沒有品項清單（特典只在圖片）→送審核")
        void imageOnly() {
            ParsedBenefitPost r = parse("""
                    《#蜘蛛人重生日》系列特別場
                    ━━━━━━ 🕷️ ━━━━━━
                    🕸️完美追擊場｜$𝟵𝟱𝟬
                    📅活動時間：𝟭𝟬/𝟵（五）、𝟭𝟬/𝟭𝟬（六）
                    📍活動影城：大巨蛋
                    ● 特典統一於入場時憑實體票及特典兌換券領取
                    """, "2026-10-05T07:06:00Z").get(0);
            assertEquals(Classification.INCOMPLETE, r.classification());
            assertTrue(r.reviewNote().contains("特典只在圖片"));
        }
    }

    @Nested
    @DisplayName("一張票好幾部電影")
    class MultiFilm {
        @Test
        @DisplayName("連映場：送審核，備註列出電影")
        void jointScreening() {
            ParsedBenefitPost r = parse("""
                    《#香巴拉的征服者》與《#嘆息之丘的聖星》兩部劇場版一次連映，
                    🎬單人套票 $𝟲𝟰𝟬
                    1 張 連映場電影票
                    ● 特典統一於入場時憑實體票及特典兌換券領取
                    """, "2026-09-29T04:30:00Z").get(0);
            assertEquals(Classification.INCOMPLETE, r.classification());
            assertEquals("連映場，含多部電影：香巴拉的征服者、嘆息之丘的聖星", r.reviewNote());
        }

        @Test
        @DisplayName("一篇列了兩部電影的場次：送審核")
        void twoTitles() {
            ParsedBenefitPost r = parse("""
                    #秀泰預售．【#IU出道18週年特別企劃】
                    《THE GOLDEN HOUR》𝟏𝟎/𝟐𝟑－𝟏𝟎/𝟐𝟗 場次
                    《THE WINNING》𝟏𝟎/𝟑𝟎－𝟏𝟏/𝟎𝟓 場次
                    凡購買活動期間《THE GOLDEN HOUR》、《THE WINNING》指定版本電影票 𝟏 張，即可於原觀影影城兌換對應電影版本之【A3 海報】𝟏 張✨
                    """, "2026-10-02T07:13:00Z").get(0);
            assertEquals(Classification.INCOMPLETE, r.classification());
            assertTrue(r.reviewNote().startsWith("含多部電影：THE GOLDEN HOUR、THE WINNING"));
        }
    }
}
