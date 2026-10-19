package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MovieNameMatcherTest {

    private final List<String> db = List.of("惡靈古堡：爆發夜", "蜘蛛人：重生日", "蜘蛛人：離家日", "Yorushika LIVE「盜作」");

    @Test
    @DisplayName("完全相同（忽略標點與大小寫）")
    void exact() {
        assertEquals(0, MovieNameMatcher.findIndex("惡靈古堡:爆發夜", db));
        assertEquals(3, MovieNameMatcher.findIndex("yorushika live 盜作", db));
    }

    @Test
    @DisplayName("只剛好一部包含才採用")
    void uniqueContains() {
        assertEquals(0, MovieNameMatcher.findIndex("惡靈古堡", db));
    }

    @Test
    @DisplayName("多部都像→送審")
    void ambiguous() {
        assertEquals(-1, MovieNameMatcher.findIndex("蜘蛛人", db));
    }

    @Test
    @DisplayName("花體字、全形英數字會先還原再比對")
    void styledLetters() {
        List<String> names = List.of("TWS VR CONCERT RUSH ROAD", "ALWAYS LALISA", "ENHYPEN VR CONCERT : DESTINY");
        assertEquals(0, MovieNameMatcher.findIndex("𝗧𝗪𝗦 𝗩𝗥 𝗖𝗢𝗡𝗖𝗘𝗥𝗧:𝗥𝗨𝗦𝗛 𝗥𝗢𝗔𝗗", names));
        assertEquals(1, MovieNameMatcher.findIndex("𝑨𝒍𝒘𝒂𝒚𝒔 𝑳𝑨𝑳𝑰𝑺𝑨", names));
        assertEquals(2, MovieNameMatcher.findIndex("𝗘𝗡𝗛𝗬𝗣𝗘𝗡 𝗩𝗥 𝗖𝗢𝗡𝗖𝗘𝗥𝗧 : 𝗗𝗘𝗦𝗧𝗜𝗡𝗬", names));
    }

    @Test
    @DisplayName("資料庫有一部叫「劇場版」的資料時，不能把所有「…劇場版…」都配過去")
    void genericWordIsNotAMovie() {
        List<String> names = List.of("劇場版", "航海王喬巴身世之謎", "鋼之鍊金術師");
        assertEquals(1, MovieNameMatcher.findIndex("航海王劇場版：喬巴身世之謎~冬季綻放、奇跡的櫻花", names));
        assertEquals(-1, MovieNameMatcher.findIndex("劇場版", List.of("劇場版X", "劇場版Y")));
    }

    @Test
    @DisplayName("找不到、空值")
    void notFound() {
        assertEquals(-1, MovieNameMatcher.findIndex("泥面人", db));
        assertEquals(-1, MovieNameMatcher.findIndex(null, db));
        assertEquals(-1, MovieNameMatcher.findIndex("泥", db));
    }
}