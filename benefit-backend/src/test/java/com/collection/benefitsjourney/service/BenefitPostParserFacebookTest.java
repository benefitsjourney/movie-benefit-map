package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class BenefitPostParserFacebookTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final BenefitPostParser parser = new BenefitPostParser();

    @Test
    void readsFacebookPhotoUriAndIgnoresVideoAndItsThumbnail() throws Exception {
        var post = mapper.readTree("""
                {
                  "url":"https://www.facebook.com/Vieshow/posts/12345",
                  "text":"本週特典：限定海報，10月14日至10月20日送完為止",
                  "pageName":"威秀影城",
                  "media":[
                    {
                      "__typename":"Photo",
                      "image":{"uri":"https://cdn.example.com/photo-original.jpg"},
                      "thumbnail":{"uri":"https://cdn.example.com/photo-thumb.jpg"}
                    },
                    {
                      "__typename":"Video",
                      "thumbnail":{"uri":"https://cdn.example.com/video-thumb.jpg"},
                      "video":{"uri":"https://cdn.example.com/video.mp4"}
                    }
                  ]
                }
                """);

        ParsedBenefitPost parsed = parser.parse(post);

        assertEquals("https://www.facebook.com/Vieshow/posts/12345", parsed.sourceUrl());
        assertTrue(parsed.imageUrls().contains("https://cdn.example.com/photo-original.jpg"));
        assertFalse(parsed.imageUrls().contains("https://cdn.example.com/video-thumb.jpg"));
        assertFalse(parsed.imageUrls().stream().anyMatch(url -> url.endsWith(".mp4")));
        assertEquals("https://cdn.example.com/photo-original.jpg", parsed.imageUrls().get(0));
    }

    @Test
    void parsesFacebookPostUrlAndTextEvenWhenThereIsNoPhoto() throws Exception {
        var post = mapper.readTree("""
                {
                  "url":"https://www.facebook.com/Vieshow/posts/98765",
                  "text":"本週特典：限定海報，10月14日至10月20日送完為止",
                  "pageName":"威秀影城",
                  "media":[{"__typename":"Video","thumbnail":{"uri":"https://cdn.example.com/video-thumb.jpg"}}]
                }
                """);

        ParsedBenefitPost parsed = parser.parse(post);

        assertEquals("https://www.facebook.com/Vieshow/posts/98765", parsed.sourceUrl());
        assertEquals(List.of(), parsed.imageUrls());
        assertTrue(parsed.classification() != ParsedBenefitPost.Classification.NOT_BENEFIT);
    }

    @Test
    void readsFacebookTimeAndParsesTextWithoutSavingVideoThumbnail() throws Exception {
        var post = mapper.readTree("""
                {
                  "postId":"1544688617704463",
                  "url":"https://www.facebook.com/reel/28585291634462760/",
                  "time":"2026-10-08T14:42:00.000Z",
                  "pageName":"vieshow",
                  "text":"活動期間，凡觀賞《Always Lalisa》不限版本之任一場次電影，即可於入場前領取「電影限定明信片」一張。",
                  "isVideo":true,
                  "media":[{
                    "__typename":"Video",
                    "thumbnail":"https://cdn.example.com/video-thumb.jpg"
                  }]
                }
                """);

        ParsedBenefitPost parsed = parser.parse(post);

        assertEquals("1544688617704463", parsed.postId());
        assertEquals(2026, parsed.publishedAt().getYear());
        assertEquals(List.of(), parsed.imageUrls());
        assertTrue(parsed.text().contains("電影限定明信片"));
        assertTrue(parsed.classification() != ParsedBenefitPost.Classification.NOT_BENEFIT);
    }

    @Test
    void doesNotTreatEveryMeetAndGreetAnnouncementAsABenefit() throws Exception {
        var post = mapper.readTree("""
                {
                  "url":"https://www.facebook.com/Vieshow/posts/12345",
                  "pageName":"vieshow",
                  "text":"導演將來到影城與影迷見面交流，映後活動結束後可憑票根簽名互動。",
                  "media":[]
                }
                """);

        ParsedBenefitPost parsed = parser.parse(post);

        assertEquals(ParsedBenefitPost.Classification.NOT_BENEFIT, parsed.classification());
    }
    
    @Test
    void parsesRealFacebookPostsFromJson() throws Exception {
        try (var input = getClass().getResourceAsStream("/facebook-posts.json")) {
            assertTrue(input != null, "找不到 facebook-posts.json");

            var posts = mapper.readTree(input);
            assertEquals(30, posts.size(), "測試資料應有 30 篇貼文");

            for (var post : posts) {
                ParsedBenefitPost parsed = parser.parse(post);

                assertEquals(
                        post.path("url").asString(""),
                        parsed.sourceUrl(),
                        "來源網址解析錯誤"
                );

                System.out.printf(
                        "分類=%s | 特典=%s | 圖片數=%d | 網址=%s%n",
                        parsed.classification(),
                        parsed.benefitName(),
                        parsed.imageUrls().size(),
                        parsed.sourceUrl()
                );
            }
        }
    }

}
