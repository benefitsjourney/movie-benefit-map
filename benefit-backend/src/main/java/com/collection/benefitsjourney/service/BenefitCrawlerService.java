package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.ImportSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 特典爬蟲主流程：Apify貼文JSON→BenefitPostParser解析→BenefitCrawlerImportService寫入待審核。
 * JSON來源可以是「上傳的檔案」或「Apify API回傳的內容」，兩者格式相同都走importPosts。
 */
@Service
public class BenefitCrawlerService {

    private final BenefitPostParser parser;
    private final BenefitCrawlerImportService importService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BenefitCrawlerService(BenefitPostParser parser, BenefitCrawlerImportService importService) {
        this.parser = parser;
        this.importService = importService;
    }

    /** 讀Apify匯出的JSON檔（內容是貼文陣列） */
    public ImportSummary importFromJson(InputStream json, boolean autoApprove) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (IOException e) {
            throw ExceptionUtils.badRequest("JSON格式錯誤：" + e.getMessage());
        }
        if (root == null || !root.isArray()) {
            throw ExceptionUtils.badRequest("JSON內容必須是貼文陣列");
        }
        return importPosts(root, autoApprove);
    }

    /** 解析每一篇貼文並寫入，單篇欄位缺漏不會中斷整批 */
    public ImportSummary importPosts(JsonNode posts, boolean autoApprove) {
        List<ParsedBenefitPost> parsed = parsePosts(posts);
        // 沒被解析成特典（NOT_BENEFIT）的貼文，Parser不會回傳，所以ignored要用總篇數補算
        ImportSummary s = importService.importAll(parsed, autoApprove);
        // 一篇貼文可能解析出多筆（例如4DX／IMAX各一款特典），所以用「有解析出東西的貼文數」來算
        long parsedPosts = parsed.stream().map(ParsedBenefitPost::sourceUrl).distinct().count();
        int ignored = Math.max(0, posts.size() - (int) parsedPosts);
        return new ImportSummary(s.created(), s.autoApproved(), s.duplicated(), s.ignored() + ignored);
    }
    
    private List<ParsedBenefitPost> parsePosts(JsonNode posts) {
        List<ParsedBenefitPost> parsed = new ArrayList<>();
        for (JsonNode post : posts) {
            String text = text(post, "text");
            String postUrl = text(post, "url");
            if (text == null || postUrl == null) {
                continue;
            }
            String image = firstImageUrl(post);
            for (ParsedBenefitPost p : parser.parseAll(
                    text(post, "postId"),
                    text(post, "facebookUrl"),
                    postUrl,
                    text(post, "pageName"),
                    text,
                    parseTime(text(post, "time")))) {
                parsed.add(image == null ? p : withImage(p, image));
            }
        }
        return parsed;
    }

    /**
     * 取貼文第一張圖的網址。Apify的media陣列每一項可能是：
     * photo_image.uri（照片）、image.uri，或只有thumbnail（影片／Reels的縮圖）。
     * 只取照片，不取影片縮圖。
     */
    static String firstImageUrl(JsonNode post) {
        JsonNode media = post.get("media");
        if (media == null || !media.isArray()) {
            return null;
        }
        for (JsonNode m : media) {
            for (String candidate : new String[] {
                    m.path("photo_image").path("uri").asText(""),
                    m.path("image").path("uri").asText("") }) {
                if (candidate.startsWith("http")) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private ParsedBenefitPost withImage(ParsedBenefitPost p, String imageUrl) {
        return new ParsedBenefitPost(p.postId(), p.sourceUrl(), p.username(), p.text(), p.publishedAt(),
                p.movieName(), p.benefitName(), p.memberOnly(), p.benefitType(), p.startDate(), p.endDate(),
                List.of(imageUrl), p.reviewNote(), p.classification(), p.chainKeyword(), p.cinemaNames());
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private Instant parseTime(String time) {
        if (time == null) {
            return null;
        }
        try {
            return Instant.parse(time);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}