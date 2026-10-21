package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.service.BenefitCrawlerImportService.ImportSummary;
import com.collection.benefitsjourney.service.sourcerules.VieshowPostRules;
import com.fasterxml.jackson.databind.ObjectMapper;

class BenefitCrawlerServiceTest {

    private BenefitCrawlerImportService importService;
    private BenefitCrawlerService service;

    @BeforeEach
    void setUp() {
        importService = mock(BenefitCrawlerImportService.class);
        service = new BenefitCrawlerService(new BenefitPostParser(List.of(new VieshowPostRules())), importService);
    }

    @Test
    @DisplayName("威秀30篇：被解析為特典或資訊不完整的貼文送進匯入，其餘算略過")
    void importFixture() throws Exception {
        when(importService.importAll(anyList(), eq(true))).thenReturn(new ImportSummary(10, 6, 0, 0));

        int total;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/vieshow30.json")) {
            total = new ObjectMapper().readTree(in).size();
        }

        try (InputStream in = getClass().getResourceAsStream("/fixtures/vieshow30.json")) {
            ImportSummary s = service.importFromJson(in, true);

            ArgumentCaptor<List<ParsedBenefitPost>> cap = ArgumentCaptor.forClass(List.class);
            verify(importService).importAll(cap.capture(), eq(true));

            int sent = cap.getValue().size();
            assertTrue(sent > 0, "至少要有貼文送進匯入");
            assertEquals(total - sent, s.ignored());
            assertEquals(10, s.created());
        }
    }

    @Test
    @DisplayName("JSON不是陣列或格式錯誤→400，不寫入")
    void badJson() {
        assertThrows(ResponseStatusException.class,
                () -> service.importFromJson(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), true));
        assertThrows(ResponseStatusException.class,
                () -> service.importFromJson(new ByteArrayInputStream("不是json".getBytes(StandardCharsets.UTF_8)), true));
        verify(importService, never()).importAll(anyList(), eq(true));
    }

    @Test
    @DisplayName("貼文圖片：優先順序photo_image.uri→image.uri，只有影片縮圖或沒有media就是空")
    void firstImageUrl() throws Exception {
        ObjectMapper om = new ObjectMapper();
        assertEquals("https://a/1.png", BenefitCrawlerService.firstImageUrl(om.readTree(
                "{\"media\":[{\"thumbnail\":\"https://a/t.png\",\"photo_image\":{\"uri\":\"https://a/1.png\"}}]}")));
        assertEquals("https://a/2.png", BenefitCrawlerService.firstImageUrl(om.readTree(
                "{\"media\":[{\"thumbnail\":\"https://a/t.png\",\"image\":{\"uri\":\"https://a/2.png\"}}]}")));
        assertNull(BenefitCrawlerService.firstImageUrl(om.readTree(
                "{\"media\":[{\"thumbnail\":\"https://a/t.png\"}]}")));
        assertNull(BenefitCrawlerService.firstImageUrl(om.readTree("{\"media\":[]}")));
        assertNull(BenefitCrawlerService.firstImageUrl(om.readTree("{}")));
    }
}