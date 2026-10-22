package com.collection.benefitsjourney.controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.service.BenefitApifyService;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService;
import com.collection.benefitsjourney.service.BenefitCrawlerLogRecorder;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.ImportSummary;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.RematchSummary;
import com.collection.benefitsjourney.service.BenefitCrawlerService;
import com.collection.benefitsjourney.service.BenefitDateShiftService;
import com.collection.benefitsjourney.service.BenefitDateShiftService.ShiftSummary;
import com.collection.benefitsjourney.service.BenefitSeedSqlService;
import com.collection.benefitsjourney.service.CloudinaryClient;
import com.collection.benefitsjourney.service.UsersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/crawler/benefits")
@Tag(name = "特典爬蟲（Admin）")
public class BenefitCrawlerController {

    private final BenefitCrawlerService benefitCrawlerService;
    private final BenefitApifyService benefitApifyService;
    private final BenefitSeedSqlService benefitSeedSqlService;
    private final BenefitCrawlerImportService benefitCrawlerImportService;
    private final BenefitDateShiftService benefitDateShiftService;
    private final CloudinaryClient cloudinaryClient;
    private final UsersService usersService;
    private final BenefitCrawlerLogRecorder crawlerLogRecorder;

    public BenefitCrawlerController(BenefitCrawlerService benefitCrawlerService,
                                    BenefitApifyService benefitApifyService,
                                    BenefitSeedSqlService benefitSeedSqlService,
                                    BenefitCrawlerImportService benefitCrawlerImportService,
                                    BenefitDateShiftService benefitDateShiftService,
                                    CloudinaryClient cloudinaryClient,
                                    UsersService usersService,
                                    BenefitCrawlerLogRecorder crawlerLogRecorder) {
        this.benefitCrawlerService = benefitCrawlerService;
        this.benefitApifyService = benefitApifyService;
        this.benefitSeedSqlService = benefitSeedSqlService;
        this.benefitCrawlerImportService = benefitCrawlerImportService;
        this.benefitDateShiftService = benefitDateShiftService;
        this.cloudinaryClient = cloudinaryClient;
        this.usersService = usersService;
        this.crawlerLogRecorder = crawlerLogRecorder;
    }

    /** 非管理員回403（做法同BenefitReportAdminController） */
    private void requireAdmin(HttpServletRequest request) {
        Integer usersId = (Integer) request.getAttribute("usersId");
        UsersBean user = usersService.getUserById(usersId);
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您沒有權限使用此功能");
        }
    }

    @PostMapping("/import-apify")
    @Operation(summary = "呼叫Apify抓Facebook貼文並匯入",
            description = "直接向Apify抓最新貼文，解析後寫入。資料齊全、電影與影城都對得上的，"
                    + "autoApprove=true時直接核准上架，其餘留在待審核。重複貼文會自動略過。"
                    + "需要先設定環境變數APIFY_TOKEN，執行時間可能要一兩分鐘。")
    public ResponseEntity<ImportSummary> importApify(
            @Parameter(description = "Facebook網址") @RequestParam(defaultValue = "https://www.facebook.com/vieshow") String pageUrl,
            @Parameter(description = "最多抓幾篇（由新到舊，上限200）") @RequestParam(defaultValue = "30") int limit,
            @Parameter(description = "只抓最近幾天內的貼文（0＝不限）。建議配合電影表涵蓋的範圍，例如21") @RequestParam(defaultValue = "0") int sinceDays,
            @RequestParam(defaultValue = "true") boolean autoApprove,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        String source = "Apify同步（" + pageUrl + "）";
        LocalDateTime start = LocalDateTime.now();
        try {
            ImportSummary summary = benefitApifyService.importFromPage(pageUrl, limit, sinceDays, autoApprove);
            crawlerLogRecorder.recordImport(source, summary, start);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            crawlerLogRecorder.recordFailure(source, e, start);
            throw e;
        }
    }

    @PostMapping(value = "/import-json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "匯入Apify貼文JSON",
            description = "上傳Apify匯出的貼文JSON並解析。資料齊全、電影與影城都對得上的，"
                    + "autoApprove=true時直接核准上架，其餘留在待審核。重複貼文會自動略過")
    public ResponseEntity<ImportSummary> importJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean autoApprove,
            HttpServletRequest httpRequest) throws IOException {

        requireAdmin(httpRequest);
        String source = "JSON匯入（" + file.getOriginalFilename() + "）";
        LocalDateTime start = LocalDateTime.now();
        try (InputStream in = file.getInputStream()) {
            ImportSummary summary = benefitCrawlerService.importFromJson(in, autoApprove);
            crawlerLogRecorder.recordImport(source, summary, start);
            return ResponseEntity.ok(summary);
        } catch (IOException | RuntimeException e) {
            crawlerLogRecorder.recordFailure(source, e, start);
            throw e;
        }
    }

    @PostMapping("/rematch")
    @Operation(summary = "重新比對待審核回報單的電影",
            description = "電影表補齊（或修正片名）之後呼叫。把「電影庫找不到對應電影」的爬蟲待審單再比對一次："
                    + "比對到電影、而且特典名／起始日／類型／影城都齊全的，直接建立正式特典並標為已上架；"
                    + "其餘維持待審核。可重複呼叫，已經處理過的不會再動。")
    public ResponseEntity<RematchSummary> rematch(HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        LocalDateTime start = LocalDateTime.now();
        try {
            RematchSummary summary = benefitCrawlerImportService.rematchPending();
            crawlerLogRecorder.recordRematch(summary, start);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            crawlerLogRecorder.recordFailure("重新比對", e, start);
            throw e;
        }
    }

    @PostMapping("/shift-dates")
    @Operation(summary = "【測試用】把所有特典的日期往後或往前平移",
            description = "測試／展示用：特典過期後地圖無法顯示，用這個把所有特典的起始日、結束日一起平移N天，"
                    + "再按照新的結束日重新判斷上架或下架。只改日期與狀態，特典ID與各種關聯都不變，可重複呼叫。"
                    + "days=0只重新判斷狀態，days可為負數；範圍 -365 到 365。")
    public ResponseEntity<ShiftSummary> shiftDates(
            @Parameter(description = "平移天數（可為負數，0＝只重新判斷狀態）") @RequestParam int days,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ResponseEntity.ok(benefitDateShiftService.shiftDates(days));
    }

    @GetMapping("/cloudinary-check")
    @Operation(summary = "檢查Cloudinary 圖片轉存的連線設定",
            description = "逐項檢查：環境變數有沒有設定、連不連得到Cloudinary、金鑰是否正確；"
                    + "另外可以帶一個圖片網址（imageUrl），測試後端是否能下載。圖片轉存失敗時先用這個找原因")
    public ResponseEntity<java.util.List<String>> cloudinaryCheck(
            @Parameter(description = "（可選）要測試下載的圖片網址") @RequestParam(required = false) String imageUrl,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ResponseEntity.ok(cloudinaryClient.diagnose(imageUrl));
    }

    @GetMapping(value = "/export-seed-sql", produces = "application/zip")
    @Operation(summary = "從資料庫產生SQL",
            description = "把資料庫裡「已上架／已下架」的正式特典，以及特典對應的電影與影城，產生成SQL並打包成zip。"
                    + "解壓縮後把benefit.sql與cinema_movie_benefit.sql放進src/main/resources/db/data/，"
                    + "資料庫是空表時，啟動就會自動匯入。待審核的回報單不會包含在內")
    public ResponseEntity<byte[]> exportSeedSql(
            @Parameter(description = "是否包含已下架（EXPIRED）的特典") @RequestParam(defaultValue = "true") boolean includeExpired,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"benefit-seed-sql.zip\"")
                .body(benefitSeedSqlService.exportZip(includeExpired));
    }
}
