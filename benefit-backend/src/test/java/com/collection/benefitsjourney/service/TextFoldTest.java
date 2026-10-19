package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextFoldTest {

    @Test
    @DisplayName("花體字與全形英數字還原成一般字")
    void foldsStyledLetters() {
        assertEquals("TWS VR CONCERT", TextFold.fold("𝗧𝗪𝗦 𝗩𝗥 𝗖𝗢𝗡𝗖𝗘𝗥𝗧"));
        assertEquals("Always LALISA 10/12", TextFold.fold("𝑨𝒍𝒘𝒂𝒚𝒔 𝑳𝑨𝑳𝑰𝑺𝑨 𝟭𝟬/𝟭𝟮"));
        assertEquals("09/24", TextFold.fold("０９/２４"));
    }

    @Test
    @DisplayName("①②③、中文與一般符號不動（解析規則還會用到 ①）")
    void keepsOtherCharacters() {
        assertEquals("① 海報 x1《電影》", TextFold.fold("① 海報 x1《電影》"));
        assertNull(TextFold.fold(null));
        assertEquals("", TextFold.fold(""));
    }
}