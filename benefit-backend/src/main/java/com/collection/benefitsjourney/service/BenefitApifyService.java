package com.collection.benefitsjourney.service;

import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.ImportSummary;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 呼叫Apify→解析→寫入待審核／自動上架
 * 手動API與排程用
 * 呼叫Apify（要等很久）在交易之外，只有寫入資料庫那段（importAll）才在交易裡，不會長時間卡住資料庫連線。
 */
@Service
public class BenefitApifyService {

    /** 設定單次最多抓幾篇，避免誤填太大浪費Apify額度 */
    static final int MAX_LIMIT = 200;
    private static final String FACEBOOK_PREFIX = "https://www.facebook.com/";

    private final ApifyClient apifyClient;
    private final BenefitCrawlerService benefitCrawlerService;

    public BenefitApifyService(ApifyClient apifyClient, BenefitCrawlerService benefitCrawlerService) {
        this.apifyClient = apifyClient;
        this.benefitCrawlerService = benefitCrawlerService;
    }

    public ImportSummary importFromPage(String pageUrl, int limit, boolean autoApprove) {
        return importFromPage(pageUrl, limit, 0, autoApprove);
    }

    /** @param sinceDays只抓最近幾天的貼文（0＝不限），電影表只有近期的片，抓太舊的貼文會一堆「電影庫找不到」 */
    public ImportSummary importFromPage(String pageUrl, int limit, int sinceDays, boolean autoApprove) {
        if (pageUrl == null || !pageUrl.startsWith(FACEBOOK_PREFIX)) {
            throw ExceptionUtils.badRequest("pageUrl必須是 " + FACEBOOK_PREFIX + " 開頭的粉專網址");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw ExceptionUtils.badRequest("limit必須介於 1 到 " + MAX_LIMIT);
        }
        if (sinceDays < 0 || sinceDays > 365) {
            throw ExceptionUtils.badRequest("sinceDays必須介於 0 到 365（0＝不限）");
        }
        JsonNode posts = apifyClient.fetchPosts(pageUrl, limit, sinceDays);
        return benefitCrawlerService.importPosts(posts, autoApprove);
    }
}