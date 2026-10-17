package com.collection.benefitsjourney.service.sourcerules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 用威秀粉專的真實貼文（fixtures/vieshow30.json）驗證規則，不需要啟動 Spring。 */
class VieshowPostRulesTest {

    private static final VieshowPostRules rules = new VieshowPostRules();
    private static JsonNode posts;

    @BeforeAll
    static void loadFixture() throws Exception {
        try (InputStream in = VieshowPostRulesTest.class.getResourceAsStream("/fixtures/vieshow30.json")) {
            posts = new ObjectMapper().readTree(in);
        }
    }

    /** 用貼文裡獨一無二的一段字找到那篇貼文的全文 */
    private static String post(String snippet) {
        String found = null;
        for (JsonNode p : posts) {
            String text = p.get("text").asText();
            if (text.contains(snippet)) {
                if (found != null) {
                    throw new IllegalStateException("片段不唯一：" + snippet);
                }
                found = text;
            }
        }
        if (found == null) {
            throw new IllegalStateException("找不到貼文：" + snippet);
        }
        return found;
    }

    @Nested
    @DisplayName("supports")
    class Supports {
        @Test
        @DisplayName("威秀粉專網址 → true；其他影城 → false")
        void supports() {
            assertTrue(rules.supports("https://www.facebook.com/vieshow/posts/pfbid0abc"));
            assertFalse(rules.supports("https://www.facebook.com/miramar/posts/123"));
            assertFalse(rules.supports(null));
        }
    }

    @Nested
    @DisplayName("一般週特典")
    class Weekly {
        @Test
        @DisplayName("盜作吊飾（標題有「開箱」也要算特典）")
        void yorushika() {
            String t = post("盜作主題壓克力吊飾");
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.REGULAR), rules.classifyBenefitType(t));
        }

        @Test
        @DisplayName("惡靈古堡第五週")
        void residentEvil() {
            String t = post("厄夜之鼠");
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.REGULAR), rules.classifyBenefitType(t));
        }

        @Test
        @DisplayName("蜘蛛人第12週")
        void spiderMan() {
            String t = post("破曉前行");
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.REGULAR), rules.classifyBenefitType(t));
        }

        @Test
        @DisplayName("removeBoilerplate 移除罐頭文後，保留標題與兌換說明")
        void removeBoilerplateKeepsBody() {
            String body = rules.removeBoilerplate(post("盜作主題壓克力吊飾"));
            assertTrue(body.contains("盜作主題壓克力吊飾"));
            assertTrue(body.contains("10/16(五)~10/22(四)"));
            assertFalse(body.contains("特典領取注意事項"));
            assertFalse(body.contains("分票"));
        }
    }

    @Nested
    @DisplayName("特別場（入場贈送型）")
    class SpecialByDate {
        @Test
        @DisplayName("泥面人4DX特別場：入場即可獲得套組")
        void nimian() {
            String t = post("面對恐懼A3工藝海報 x1");
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.SPECIAL_BY_DATE), rules.classifyBenefitType(t));
        }

        @Test
        @DisplayName("我們不是什麼特典場：本場限定素描海報")
        void notSomething() {
            String t = post("全球唯一的素描版海報");
            assertEquals(Classification.BENEFIT, rules.classifyPost(t));
            assertEquals(Optional.of(BenefitType.SPECIAL_BY_DATE), rules.classifyBenefitType(t));
        }
    }

    @Nested
    @DisplayName("資訊不完整（INCOMPLETE）")
    class Incomplete {
        @Test
        @DisplayName("ATEEZ應援場：沒有講贈品")
        void ateez() {
            assertEquals(Classification.INCOMPLETE, rules.classifyPost(post("光感應援場")));
        }

        @Test
        @DisplayName("人魚戀習曲導演映後場：沒有講贈品")
        void director() {
            assertEquals(Classification.INCOMPLETE, rules.classifyPost(post("導演映後場")));
        }

        @Test
        @DisplayName("Stray Kids：活動限定入場特典「即將公開」（兩篇）")
        void strayKids() {
            assertEquals(Classification.INCOMPLETE, rules.classifyPost(post("即將啟售")));
            assertEquals(Classification.INCOMPLETE, rules.classifyPost(post("情報解禁")));
        }
    }

    @Nested
    @DisplayName("不是特典（NOT_BENEFIT）")
    class NotBenefit {
        @Test
        @DisplayName("本週購票特典懶人包")
        void roundup() {
            assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(post("本週購票特典懶人包")));
        }

        @Test
        @DisplayName("航海王特典更換公告（有「原購票影城」字樣也不能算新特典）")
        void replaceNotice() {
            assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(post("特典更換公告")));
        }

        @Test
        @DisplayName("一般宣傳貼文")
        void ordinaryPosts() {
            for (String s : List.of("Tsubaki Factory", "奧德賽爆米花桶", "宛如星辰的你", "另一個媽咪", "復仇者聯盟")) {
                assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(post(s)), s);
            }
        }

        @Test
        @DisplayName("空字串與 null")
        void blank() {
            assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(""));
            assertEquals(Classification.NOT_BENEFIT, rules.classifyPost(null));
            assertEquals(Optional.empty(), rules.classifyBenefitType(null));
        }
    }

    @Nested
    @DisplayName("尚未支援（之後做限量兌換型再打開）")
    class Deferred {
        @Disabled("限量兌換型還沒做：哥吉拉首映、LISA明信片、快打旋風POSE挑戰，目前會被判成 NOT_BENEFIT")
        @Test
        void limitedRedeem() {
            assertEquals(Classification.BENEFIT, rules.classifyPost(post("特殊工藝日本原版A3海報+神秘驚喜禮")));
            assertEquals(Classification.BENEFIT, rules.classifyPost(post("電影限定明信片")));
            assertEquals(Classification.BENEFIT, rules.classifyPost(post("BLANKA 娃娃購物袋")));
        }
    }
}
