package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CloudinaryClientTest {

    @Test
    @DisplayName("簽名：參數照字母排序（folder、timestamp），後面直接接API Secret再做SHA-1")
    void signWithFolder() {
        assertEquals("c99a2cada9bd0c856bf1f81068034da8fc1e5e3c",
                CloudinaryClient.sign("benefits", 1700000000L, "abc"));
    }

    @Test
    @DisplayName("簽名：沒指定資料夾就只簽timestamp")
    void signWithoutFolder() {
        assertEquals("0c057bd2f2ef882ea4abe379aa16139064aab947",
                CloudinaryClient.sign("", 1700000000L, "abc"));
    }

    @Test
    @DisplayName("沒設定Cloudinary金鑰：不上傳，回空值，不會丟例外")
    void notConfigured() {
        CloudinaryClient client = new CloudinaryClient();
        assertFalse(client.isConfigured());
        Optional<CloudinaryClient.UploadedImage> r = client.uploadFromUrl("https://example.com/a.png");
        assertTrue(r.isEmpty());
    }
}