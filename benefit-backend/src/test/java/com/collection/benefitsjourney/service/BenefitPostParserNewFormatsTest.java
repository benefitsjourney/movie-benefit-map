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

/** 威秀200篇實測後新增的貼文格式（兌換資訊、預購禮、會員禮、花體字片名、❶❷條列、IMAX深夜場） */
class BenefitPostParserNewFormatsTest {

    private static final BenefitPostParser parser = new BenefitPostParser(List.of(new VieshowPostRules()));
    private static final String PAGE = "https://www.facebook.com/vieshow";

    private static ParsedBenefitPost parse(String text, String time) {
        List<ParsedBenefitPost> r = parser.parseAll("id", PAGE, PAGE + "/posts/x", "vieshow", text, Instant.parse(time));
        assertEquals(1, r.size());
        return r.get(0);
    }

    private static final String POST_143 = """
不管吃月餅吃到多重，這個邪，還是要粽...!
這個中秋一次收藏！#粽邪4 系列海報

━━━ 📝 兌換資訊 ━━━
🗓 場次期間： 09/25(五)-10/01(四)
🎟 兌換資格：期間內任一場次《粽邪4：坤蒂拉娜》電影票 1張
🎁 贈品內容：#粽邪系列前三集 A3官方海報 各1張（共3張）🎬
📍 兌換時間：09/25(五)白天營業時間起，憑票至「原購票影城」領取
⚠️ 數量有限，送完為止！

━━━ 📌 注意事項 ━━━
❶非該特典活動期間的票根不可兌換，限至原購票影城兌換。贈品發放以現場兌換順序為主（無論場次先後），網路及APP購票者請盡早至觀影影城取票兌換，數量有限，送完為止。
❷贈品以實物為主，兌換時請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理。
❸團劃包廳、免費票種（包含：兌點票/忠誠禮/電影票根價格為0元的票種）不可兌換。
❹購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。

📍使用APP訂票並選擇電子票券者：
1️⃣請點選「會員」專區 >「訂單紀錄」>「顯示贈品條碼」供影城員工掃描領取贈品。
2️⃣進行分票之購票交易，僅限「主購票人」領取贈品，分票受贈者無法領取。
3️⃣若有多款/份贈品須一次領齊，恕無法分開領取。

━━━ 🎟️ 立即訂票 ━━━

#威秀影城 #VieshowCinemas
""";

    @Test
    @DisplayName("兌換資訊格式（沒有「凡購買」）")
    void post143() {
        ParsedBenefitPost r = parse(POST_143, "2026-09-23T13:00:36.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("粽邪4：坤蒂拉娜", r.movieName());
        assertEquals("粽邪系列前三集 A3官方海報", r.benefitName());
        assertEquals("REGULAR", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 9, 25, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 1, 23, 59, 59), r.endDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_145 = """
⋆.˚手心搓十下，放在耳朵旁邊，聽海的聲音⋆.˚

━━━ 📝 《#驀然回首 真人版》第三週特典兌換資訊 ━━━
🖼️ 特典內容：【聽海款電影工藝海報】1張
🎟️ 兌換資格：09/25(五)-10/01(四) 期間，購買《驀然回首 真人版》電影票1張
📍 兌換時間／地點：09/25(五)白天營業時間起，憑電影票至原購票影城兌換領取
⚠️ 數量有限，送完為止！

━━━ 📌 注意事項 ━━━
❶非該入場特典活動期間之票根不可兌換，限至原購票影城兌換。贈品發放以現場兌換順序為主（無論場次先後），網路及APP購票者請盡早至觀影影城取票兌換，數量有限，送完為止。
❷每人限兌4張，贈品以實物為主，兌換時請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理。
❸電影交換券、電影預售票適用本活動；團劃包廳、免費票種（包含：兌點票/忠誠禮/電影票根價格為0元的票種）不可兌換。
❹購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。

📍使用APP訂票並選擇電子票券者：
1️⃣請點選「會員」專區 >「訂單紀錄」>「顯示贈品條碼」供影城員工掃描領取贈品。
2️⃣進行分票之購票交易，僅限「主購票人」領取贈品，分票受贈者無法領取。

━━━ 🎟️ 立即訂票 ━━━

#威秀影城 #VieshowCinemas
""";

    @Test
    @DisplayName("特典內容：【…】格式")
    void post145() {
        ParsedBenefitPost r = parse(POST_145, "2026-09-23T11:00:15.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("驀然回首 真人版", r.movieName());
        assertEquals("聽海款電影工藝海報", r.benefitName());
        assertEquals("REGULAR", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 9, 25, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 1, 23, 59, 59), r.endDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_146 = """
《#網紅老爸》#PAPAGO限量彩蛋款海報
把老爸的溫柔帶回家~
凡購買 09/25(五)-10/01(四) 期間《網紅老爸》任一場次電影票 1 張，於 09/25(五) 白天營業時間起至原購票影城，即可兌換「PAPAGO 限量彩蛋款」A3 海報 1 張♡
✦ 海報共 2 款，隨機贈送、恕不挑款
✦ 每張電影票限兌換 1 份，每人每次限兌換 4 份喔~
✦ 數量有限，送完為止

━━━ 📌 注意事項 ━━━
❶非該入場特典活動期間的票根不可兌換，限至原購票影城兌換。贈品發放以現場兌換順序為主（無論場次先後），網路及APP購票者請盡早至觀影影城取票兌換，數量有限，送完為止。
❷每人限兌4張，海報共有二款，款式隨機發送，恕不挑款。贈品以實物為主，兌換時請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理。
❸電影交換券適用兌換。團體劃位、包廳、特別場及免費票種（包含兌點票、忠誠禮、電影票根金額為 0 元之票種）恕不適用本活動。
❹購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。

📍使用APP訂票並選擇電子票券者：
1️⃣請點選「會員」專區 >「訂單紀錄」>「顯示贈品條碼」供影城員工掃描領取贈品。
2️⃣進行分票之購票交易，僅限「主購票人」領取贈品，分票受贈者無法領取。

━━━ 🎟️ 立即訂票 ━━━

#威秀影城 #VieshowCinemas
""";

    @Test
    @DisplayName("兌換「…」A3 海報：括號後面的品項也算特典名")
    void post146() {
        ParsedBenefitPost r = parse(POST_146, "2026-09-23T10:00:09.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("網紅老爸", r.movieName());
        assertEquals("PAPAGO 限量彩蛋款 A3 海報", r.benefitName());
        assertEquals("REGULAR", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 9, 25, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 1, 23, 59, 59), r.endDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_120 = """
🎈《劇場版 吉伊卡哇 人魚島的秘密》 #第10週特典 #自動充氣氣球 登場.ᐟ.ᐟ
全12款隨機贈送，這週跟大家一起「飄」進人魚島吧～ ꒰ᐢ. .ᐢ꒱♡

⭐凡10/02(五)～10/09(五)期間，購買任一版本本場次電影票１張，於10/02(五)白天營業時間起至原購票影城，即可兌換領取【自動充氣氣球】乙個！
🎈總共12款，隨機贈送一款，恕不提供挑選
🎈一張電影票贈一個，每人限兌4個，數量有限，送完為止

◢◤特典領取注意事項◢◤
❶非該入場特典活動期間與指定版本的票根不可兌換，請於活動指定開放領取時間限至原購票影城兌換。不可跨版本兌換，贈品發放以現場兌換順序為主（無論場次先後），數量有限，送完為止。
❷每人限兌4本/4張，贈品以實物為主，兌換時請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理。
❸ 貴賓券、團劃包廳、免費票種（包含：兌點票/忠誠禮/電影票根價格為0元的票種）不可兌換。
❹購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。

📍使用APP訂票並選擇電子票券者：
1.請點選「會員」專區 >「訂單紀錄」>「顯示贈品條碼」供影城員工掃描領取贈品。
2.進行分票之購票交易，僅限「主購票人」領取贈品，分票受贈者無法領取。
3.若有多款/份贈品須一次領齊，恕無法分開領取。
""";

    @Test
    @DisplayName("「凡10/02～10/09期間，購買…」：期間在前、購買在後")
    void post120() {
        ParsedBenefitPost r = parse(POST_120, "2026-09-29T02:45:03.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("劇場版 吉伊卡哇 人魚島的秘密", r.movieName());
        assertEquals("自動充氣氣球", r.benefitName());
        assertEquals("REGULAR", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 9, 23, 59, 59), r.endDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_85 = """
🩸《#另一個媽咪》#首週特典 登場！
「媽咪」張開雙手迎接你……但你確定，那真的是你的媽咪嗎？👀
海報上的紅光跟媽咪，光看就讓人瑟瑟發抖.......꒰˚ ˃̣̣̥⌓˂̣̣̥ ꒱

★ 凡場次開放售票起，購買10/08(四)起～10/14(三)期間《另一個媽咪》任一版本場次電影票１張，至原購票影城即可兌換領取「限量主視覺款A3海報」１張。

── ✦── ✦ ── ✦ ── ✦ ──

🎟️ ꒰ 特典領取注意事項 ꒱
✦ 非該入場特典活動期間的票根不可兌換，限至原購票影城兌換。贈品發放以現場兌換順序為主（無論場次先後），網路及APP購票者請盡早至觀影影城取票兌換，數量有限，送完為止。
✦ 每人限兌4張，贈品以實物為主，兌換時請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理。
✦ 電影交換券、團劃包廳、免費票種（包含：兌點票/忠誠禮/電影票根價格為0元的票種）不可兌換。
✦ 購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。

📱 ꒰ 使用APP訂票並選擇電子票券者 ꒱
✧ 請點選「會員」專區 ➜「訂單紀錄」➜「顯示贈品條碼」供影城員工掃描領取贈品。
✧ 進行分票之購票交易，僅限「主購票人」領取贈品，分票受贈者無法領取。
""";

    @Test
    @DisplayName("「購買10/08(四)起～10/14(三)期間」沒有「凡」")
    void post85() {
        ParsedBenefitPost r = parse(POST_85, "2026-10-05T13:00:10.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("另一個媽咪", r.movieName());
        assertEquals("限量主視覺款A3海報", r.benefitName());
        assertEquals("REGULAR", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 10, 8, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 14, 23, 59, 59), r.endDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_189 = """
🩷 有些回憶，絕對需要你勇敢守住 ！
原創動畫【#誤闖遺忘島】09/18(五)開放預售

🎁預購電影票即可獲得「預售專屬A3特典」1張。
🍿電影套餐09/25(五)上市

⚠️注意事項：
1.預購完成後無論場次先後，即可至原購票影城櫃台領取預購禮(不得跨影城領取)，數量有限，送完為止，先領先贏。
2.贈品以實物為主，圖片僅供參考。兌換時，請於現場確認品項狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理，如贈品毀損恕無法辦理退票。
3.特別場票根、團劃包廳、免費票券（包含：兌點票/忠誠禮/電影票根價格為0元的票種）均不適用兌換。
4.購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。

📍使用官方APP訂票並選擇電子票券者：
1.請點選「會員」專區 >「訂單紀錄」>「顯示贈品條碼」供影城員工掃描領取贈品。
2.進行分票之購票交易，僅限「主購票人」領取贈品，分票受贈者無法領取。
""";

    @Test
    @DisplayName("預購禮：預購電影票即可獲得「…」→從預售日起、送完為止、全連鎖")
    void preorderGift() {
        ParsedBenefitPost r = parse(POST_189, "2026-09-17T04:00:24.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("預售專屬A3特典", r.benefitName());
        assertEquals("SPECIAL_UNTIL_OUT", r.benefitType());
        assertEquals(LocalDateTime.of(2026, 9, 18, 0, 0), r.startDate());
        assertNull(r.endDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_91 = """
㊙️iShow會員請留意您的會員信箱
【《#另一個媽咪》獨家驚嚇禮已送達📧】

🙃10/08(四)起至威秀影城觀賞《另一個媽咪》
並臨櫃出示「電影票🎫+活動EDM📜」
即可兌換🎁【獨家｜電影A3 特寫款海報】1張。
（※數量有限換完為止）

⚠️注意事項：
1.#本活動僅限收到活動EDM之會員參與。活動EDM於10/05(一)起，陸續寄送至iShow會員電子信箱。
2.#限至原購票之活動影城兌換，海報發放以現場兌換順序為主，數量有限，送完為止。
3.臨櫃出示電影票根１張和活動EDM１份，經工作人員確認後蓋手章即可領取。#不論電影票張數僅限領一張。
4.團劃包廳、免費票種（電影票根價格為0元的票種）不可兌換。
5.兌換時請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理。
6.購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件「正本」供影城核對，恕不接受任何形式之代領。
7.威秀影城與MUVIE CINEMAS擁有保留修改活動內容之權利以及最終解釋權。
""";

    @Test
    @DisplayName("iShow會員EDM兌換：標成會員禮，不是抽獎")
    void memberOnlyGift() {
        ParsedBenefitPost r = parse(POST_91, "2026-10-05T03:18:22.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals(Boolean.TRUE, r.memberOnly());
        assertEquals("獨家｜電影A3 特寫款海報", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 10, 8, 0, 0), r.startDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_131 = """
《𝑨𝒍𝒘𝒂𝒚𝒔 𝑳𝑨𝑳𝑰𝑺𝑨》
𝟭𝟬/𝟭𝟮、𝟭𝟬/𝟭𝟰、𝟭𝟬/𝟭𝟳｜𝟮𝗗, 𝗜𝗠𝗔𝗫 𝟮𝗗同步上映

活動期間，觀賞《Always Lalisa》不限版本之任一場次電影，即可於入場前領取「電影限定明信片」一張。

╰┈➤#現正熱售中  購票連結

#LISA #AlwaysLalisa
#威秀影城娛樂企劃
""";

    @Test
    @DisplayName("花體字片名會還原成一般英文；「不限版本」提到IMAX也算全連鎖；放映日期當活動期間")
    void styledMovieName() {
        ParsedBenefitPost r = parse(POST_131, "2026-09-24T10:00:36.000Z");
        assertEquals("Always LALISA", r.movieName());
        assertEquals("電影限定明信片", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 10, 12, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 17, 23, 59, 59), r.endDate());
        assertEquals("威秀", r.chainKeyword());
    }

    private static final String POST_141 = """
《惡靈古堡：爆發夜》深夜顫慄「加碼情報」💥📝
IMAX深夜場限定活動🎁延長加碼至中秋節連假🫶

☢️凡購買09/24(四)~09/27(日)期間，指定活動影城「深夜最後一場IMAX版本電影票」，即可於入場時{{保證獲得}}「小心惡犬|A5透卡」1張。
📍活動影城：板橋大遠百/新店裕隆城/台南南紡/高雄大遠百

⚠️ #特典統一於該場次入場時發放，為維持影城兌換流程順暢，採隨機發放礙難提供挑選。
⚠️特典以實物為主，請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理。
⚠️特別場票根、團劃包廳、免費票種（包含：兌點票/忠誠禮/電影票根價格為0元的票種）不可兌換。
⚠購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。
⚠最終場定義為活動影城營業日最後一場次，如有跨夜場次官方APP將顯示於隔日。
""";

    @Test
    @DisplayName("IMAX深夜場「保證獲得」：期間＋活動影城")
    void imaxLateNight() {
        ParsedBenefitPost r = parse(POST_141, "2026-09-23T15:01:51.000Z");
        assertEquals(Classification.BENEFIT, r.classification());
        assertEquals("小心惡犬|A5透卡", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 9, 24, 0, 0), r.startDate());
        assertEquals(LocalDateTime.of(2026, 9, 27, 23, 59, 59), r.endDate());
        assertEquals(4, r.cinemaNames().size());
    }

    private static final String POST_158 = """
【航海王映畫祭】《航海王劇場版：喬巴身世之謎~冬季綻放、奇跡的櫻花》觀影特典情報✊

⚓️於影城開放場次售票起，凡購買09/25起《航海王劇場版：喬巴身世之謎~冬季綻放、奇跡的櫻花》任一場次電影票１張，即可至原購票影城臨櫃兌換：
❶ 特殊工藝A3海報１張
❷ 角色小立牌盲包（共3款，款式隨機） １個
※數量有限，送完為止；每張票限兌換各1個，至多各4個

⚠️特典領取注意事項⚠️
❶贈品發放以現場兌換順序為主（無論場次先後），數量有限，送完為止。
❷特別場票根、電影交換券、團劃包廳、影城免費兌換券（包含：兌點票/忠誠禮/電影票根價格為0元的票種）不可兌換特典。
❸ 贈品以實物為主，兌換時請於現場確認贈品狀況，離場後恕不接受更換。如有退/換票需連同贈品一併辦理退換票手續。
❹購買「敬老/愛心票」者，領取贈品時須由「本人」出示有效身分證件正本供影城核對，恕不接受任何形式之代領。

📍使用APP訂票並選擇電子票券者：
1.請點選「會員」專區 >「訂單紀錄」>「顯示贈品條碼」供影城員工掃描領取。
2.多款/份贈品須一次領齊，恕無法分開領取。
3.進行分票之購票交易，僅限「主購票人」領取贈品，分票受贈者無法領取。
""";

    @Test
    @DisplayName("兌換：❶❷ 條列：只收特典，不收「不可兌換」之類的規則行")
    void numberedGifts() {
        ParsedBenefitPost r = parse(POST_158, "2026-09-22T06:00:27.000Z");
        assertEquals("特殊工藝A3海報、角色小立牌盲包", r.benefitName());
        assertEquals(LocalDateTime.of(2026, 9, 25, 0, 0), r.startDate());
    }

    @Test
    @DisplayName("雜訊：VR演唱會／直播／猜謎不該送審核")
    void noiseIgnored() {
        String vr = "🔥 #現正熱映中 #TWS 𝗩𝗥演唱會 💙\n搶先帶 #４２ 體驗《𝗧𝗪𝗦 𝗩𝗥 𝗖𝗢𝗡𝗖𝗘𝗥𝗧:𝗥𝗨𝗦𝗛 𝗥𝗢𝗔𝗗》\n🎁 特典 明信片 倒數2天";
        assertTrue(parser.parseAll("id", PAGE, PAGE + "/posts/vr", "vieshow", vr, Instant.parse("2026-10-16T01:00:00Z")).isEmpty());
    }
}