package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.BenefitReportBean;
import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.repository.BenefitReportRepository;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.ImportSummary;
import com.collection.benefitsjourney.service.BenefitCrawlerImportService.RematchSummary;
import com.collection.benefitsjourney.service.CloudinaryClient.UploadedImage;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;

@ExtendWith(MockitoExtension.class)
class BenefitCrawlerImportServiceTest {

    @Mock BenefitReportRepository reportRepo;
    @Mock BenefitRepository benefitRepo;
    @Mock CinemaMovieBenefitRepository linkRepo;
    @Mock MovieRepository movieRepo;
    @Mock CinemaRepository cinemaRepo;
    @Mock CloudinaryClient cloudinary;
    @InjectMocks BenefitCrawlerImportService service;

    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

    private MovieBean resident;
    private CinemaBean xinyi;
    private CinemaBean songren;
    private CinemaBean showtimes;

    @BeforeEach
    void setUp() {
        resident = new MovieBean();
        resident.setMovieId(35);
        resident.setMovieName("惡靈古堡：爆發夜");
        xinyi = cinema(1, "台北信義威秀");
        songren = cinema(2, "MUVIE CINEMAS台北松仁威秀");
        showtimes = cinema(8, "大巨蛋秀泰影城");
    }

    private CinemaBean cinema(int id, String name) {
        CinemaBean c = new CinemaBean();
        c.setCinemaId(id);
        c.setCinemaName(name);
        c.setIsActive(true);
        return c;
    }

    /** 週特典（全威秀），結束日可指定，避免測試因為「現在幾號」而時好時壞 */
    private ParsedBenefitPost weekly(String movie, String gift, LocalDateTime end) {
        return new ParsedBenefitPost("id", "https://www.facebook.com/vieshow/posts/1", "vieshow", "text",
                LocalDateTime.of(2026, 10, 12, 19, 0), movie, gift, Boolean.FALSE, "REGULAR",
                LocalDateTime.of(2026, 10, 16, 0, 0), end,
                List.of(), "爬蟲自動建立，請確認電影與日期", Classification.BENEFIT, "威秀", List.of());
    }

    private ParsedBenefitPost weekly(String movie, String gift) {
        return weekly(movie, gift, FAR_FUTURE);
    }

    /** 特別場（指定影城） */
    private ParsedBenefitPost special(String movie, List<String> cinemas) {
        return new ParsedBenefitPost("id", "https://www.facebook.com/vieshow/posts/2", "vieshow", "text",
                LocalDateTime.of(2026, 10, 12, 19, 0), movie, "特別場套組", Boolean.FALSE, "SPECIAL_BY_DATE",
                LocalDateTime.of(2026, 10, 24, 0, 0), FAR_FUTURE,
                List.of(), "爬蟲自動建立，請確認電影與日期", Classification.BENEFIT, null, cinemas);
    }

    private ParsedBenefitPost incomplete() {
        return new ParsedBenefitPost("id", "https://www.facebook.com/vieshow/posts/3", "vieshow", "text",
                LocalDateTime.of(2026, 10, 12, 19, 0), "泥面人", null, Boolean.FALSE, "SPECIAL_BY_DATE",
                null, null, List.of(), "資訊不完整", Classification.INCOMPLETE, null, List.of());
    }

    private void givenData() {
        when(movieRepo.findAll()).thenReturn(List.of(resident));
        when(cinemaRepo.findAll()).thenReturn(List.of(xinyi, songren, showtimes));
    }

    /** save時補上主鍵，模擬資料庫產生ID */
    private void givenBenefitSaveAssignsId(int id) {
        when(benefitRepo.save(any())).thenAnswer(i -> {
            BenefitBean b = i.getArgument(0);
            b.setBenefitId(id);
            return b;
        });
    }

    @Test
    @DisplayName("週特典、電影對得上：直接建立正式特典＋中介表，不寫審核回報")
    void autoCreateWeekly() {
        givenData();
        givenBenefitSaveAssignsId(10);

        ImportSummary s = service.importAll(List.of(weekly("惡靈古堡：爆發夜", "厄夜之鼠款A3夜光海報")), true);

        verify(reportRepo, never()).save(any());

        ArgumentCaptor<BenefitBean> benefit = ArgumentCaptor.forClass(BenefitBean.class);
        verify(benefitRepo).save(benefit.capture());
        assertEquals("厄夜之鼠款A3夜光海報", benefit.getValue().getBenefitName());
        assertEquals("APPROVED", benefit.getValue().getStatus());
        assertEquals("REGULAR", benefit.getValue().getBenefitType());
        assertEquals("https://www.facebook.com/vieshow/posts/1", benefit.getValue().getSourceUrl());
        assertEquals("爬蟲自動建立", benefit.getValue().getReviewNote());     // 已上架就不用再寫「請確認電影與日期」

        ArgumentCaptor<CinemaMovieBenefitBean> link = ArgumentCaptor.forClass(CinemaMovieBenefitBean.class);
        verify(linkRepo, times(2)).save(link.capture());                 // 秀泰不含「威秀」，不綁
        assertEquals(List.of(1, 2), link.getAllValues().stream().map(l -> l.getCinema().getCinemaId()).toList());
        assertSame(resident, link.getAllValues().get(0).getMovie());
        assertEquals(10, link.getAllValues().get(0).getBenefit().getBenefitId());

        assertEquals(new ImportSummary(1, 1, 0, 0), s);
    }

    @Test
    @DisplayName("結束日已經過了：直接設為已下架（EXPIRED）")
    void expiredWhenEndPassed() {
        givenData();
        givenBenefitSaveAssignsId(10);

        service.importAll(List.of(weekly("惡靈古堡：爆發夜", "海報", LocalDateTime.of(2020, 1, 1, 0, 0))), true);

        ArgumentCaptor<BenefitBean> benefit = ArgumentCaptor.forClass(BenefitBean.class);
        verify(benefitRepo).save(benefit.capture());
        assertEquals("EXPIRED", benefit.getValue().getStatus());
    }

    @Test
    @DisplayName("同電影已有同名上架特典：沿用，不重複建立，只補中介表")
    void reuseExistingBenefit() {
        givenData();
        BenefitBean existing = new BenefitBean();
        existing.setBenefitId(5);
        when(benefitRepo.findApprovedByMovieAndName(35, "海報")).thenReturn(List.of(existing));

        ImportSummary s = service.importAll(List.of(weekly("惡靈古堡：爆發夜", "海報")), true);

        verify(benefitRepo, never()).save(any());
        ArgumentCaptor<CinemaMovieBenefitBean> link = ArgumentCaptor.forClass(CinemaMovieBenefitBean.class);
        verify(linkRepo, times(2)).save(link.capture());
        assertSame(existing, link.getAllValues().get(0).getBenefit());
        assertEquals(1, s.autoApproved());
    }

    @Test
    @DisplayName("中介表已存在的影城不重複綁")
    void skipExistingLink() {
        givenData();
        givenBenefitSaveAssignsId(10);
        when(linkRepo.existsByCinema_CinemaIdAndMovie_MovieIdAndBenefit_BenefitId(eq(1), any(), any())).thenReturn(true);
        when(linkRepo.existsByCinema_CinemaIdAndMovie_MovieIdAndBenefit_BenefitId(eq(2), any(), any())).thenReturn(false);

        service.importAll(List.of(weekly("惡靈古堡：爆發夜", "海報")), true);

        ArgumentCaptor<CinemaMovieBenefitBean> link = ArgumentCaptor.forClass(CinemaMovieBenefitBean.class);
        verify(linkRepo).save(link.capture());
        assertEquals(2, link.getValue().getCinema().getCinemaId());
    }

    @Test
    @DisplayName("autoApprove=false：只寫入待審核回報單，不建立正式特典")
    void noAutoApprove() {
        givenData();

        ImportSummary s = service.importAll(List.of(weekly("惡靈古堡：爆發夜", "海報")), false);

        ArgumentCaptor<BenefitReportBean> cap = ArgumentCaptor.forClass(BenefitReportBean.class);
        verify(reportRepo).save(cap.capture());
        assertEquals("PENDING", cap.getValue().getStatus());
        assertSame(resident, cap.getValue().getMovie());
        assertNull(cap.getValue().getUser());
        verify(benefitRepo, never()).save(any());
        verify(linkRepo, never()).save(any());
        assertEquals(new ImportSummary(1, 0, 0, 0), s);
    }

    @Test
    @DisplayName("特別場：只綁貼文列出的影城；有影城對不上時寫在特典備註，但仍上架")
    void specialWithListedCinemas() {
        givenData();
        givenBenefitSaveAssignsId(7);

        ImportSummary s = service.importAll(List.of(special("惡靈古堡：爆發夜", List.of("台北信義", "不存在影城"))), true);

        ArgumentCaptor<CinemaMovieBenefitBean> link = ArgumentCaptor.forClass(CinemaMovieBenefitBean.class);
        verify(linkRepo).save(link.capture());
        assertEquals(1, link.getValue().getCinema().getCinemaId());
        ArgumentCaptor<BenefitBean> benefit = ArgumentCaptor.forClass(BenefitBean.class);
        verify(benefitRepo).save(benefit.capture());
        assertTrue(benefit.getValue().getReviewNote().contains("未對應影城：不存在影城"));
        assertEquals(1, s.autoApproved());
    }

    @Test
    @DisplayName("影城一間都對不上：不上架，留在待審核")
    void noCinemaMatched() {
        givenData();

        ImportSummary s = service.importAll(List.of(special("惡靈古堡：爆發夜", List.of("不存在影城"))), true);

        verify(benefitRepo, never()).save(any());
        verify(reportRepo).save(any());
        assertEquals(new ImportSummary(1, 0, 0, 0), s);
    }

    @Test
    @DisplayName("電影庫找不到電影：改存customMovieName，留在待審核")
    void movieNotFound() {
        givenData();

        service.importAll(List.of(weekly("泥面人", "海報")), true);

        ArgumentCaptor<BenefitReportBean> cap = ArgumentCaptor.forClass(BenefitReportBean.class);
        verify(reportRepo).save(cap.capture());
        assertNull(cap.getValue().getMovie());
        assertEquals("泥面人", cap.getValue().getCustomMovieName());
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("INCOMPLETE（沒有特典名）：存成待審核，不上架")
    void incompleteStaysPending() {
        givenData();

        ImportSummary s = service.importAll(List.of(incomplete()), true);

        assertEquals(new ImportSummary(1, 0, 0, 0), s);
        verify(reportRepo).save(any());
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("同貼文＋同特典名已有回報單：不重複寫入")
    void duplicateByReportUrl() {
        when(movieRepo.findAll()).thenReturn(List.of());
        when(cinemaRepo.findAll()).thenReturn(List.of());
        when(reportRepo.existsBySourceUrlAndBenefitName(any(), any())).thenReturn(true);

        ImportSummary s = service.importAll(List.of(weekly("A片", "特典")), true);

        assertEquals(new ImportSummary(0, 0, 1, 0), s);
        verify(reportRepo, never()).save(any());
    }

    @Test
    @DisplayName("同貼文＋同特典名已有正式特典（上次自動上架的）：不重複寫入")
    void duplicateByBenefitUrl() {
        when(movieRepo.findAll()).thenReturn(List.of());
        when(cinemaRepo.findAll()).thenReturn(List.of());
        when(benefitRepo.existsBySourceUrlAndBenefitName(any(), any())).thenReturn(true);

        ImportSummary s = service.importAll(List.of(weekly("A片", "特典")), true);

        assertEquals(new ImportSummary(0, 0, 1, 0), s);
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("特典名超過100字：用截斷後的名字去重（資料庫存的就是截斷後的）")
    void duplicateCheckUsesTruncatedName() {
        when(movieRepo.findAll()).thenReturn(List.of());
        when(cinemaRepo.findAll()).thenReturn(List.of());
        String longName = "海".repeat(150);
        when(reportRepo.existsBySourceUrlAndBenefitName(
                "https://www.facebook.com/vieshow/posts/1", "海".repeat(100))).thenReturn(true);

        ImportSummary s = service.importAll(List.of(weekly("A片", longName)), true);

        assertEquals(new ImportSummary(0, 0, 1, 0), s);
    }

    @Test
    @DisplayName("不同貼文重複宣傳同一部電影的同一個特典（回報單已有）：只留第一筆")
    void duplicateBySemanticReport() {
        givenData();
        when(reportRepo.existsByMovie_MovieIdAndBenefitNameAndStartDate(eq(35), eq("海報"), any())).thenReturn(true);

        ImportSummary s = service.importAll(List.of(weekly("惡靈古堡：爆發夜", "海報")), true);

        assertEquals(new ImportSummary(0, 0, 1, 0), s);
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("不同貼文重複宣傳同一部電影的同一個特典（正式特典已有）：只留第一筆")
    void duplicateBySemanticBenefit() {
        givenData();
        when(benefitRepo.countByMovieAndNameAndStartDate(eq(35), eq("海報"), any())).thenReturn(1L);

        ImportSummary s = service.importAll(List.of(weekly("惡靈古堡：爆發夜", "海報")), true);

        assertEquals(new ImportSummary(0, 0, 1, 0), s);
        verify(benefitRepo, never()).save(any());
        verify(reportRepo, never()).save(any());
    }

    @Test
    @DisplayName("NOT_BENEFIT一律略過")
    void ignored() {
        givenData();
        ParsedBenefitPost p = new ParsedBenefitPost("id", "u", "vieshow", "t", null, null, null, null, null,
                null, null, List.of(), null, Classification.NOT_BENEFIT, null, List.of());

        ImportSummary s = service.importAll(List.of(p), true);

        assertEquals(new ImportSummary(0, 0, 0, 1), s);
        verify(reportRepo, never()).save(any());
        verify(benefitRepo, never()).save(any());
    }

    // ===== 重新比對 =====

    /** 爬蟲建立、電影庫找不到電影的待審單（影城1,2是匯入當時比對到的） */
    private BenefitReportBean pendingNoMovie(String customMovie, String cinemaIds) {
        BenefitReportBean r = new BenefitReportBean();
        r.setStatus("PENDING");
        r.setBenefitName("厄夜之鼠款A3夜光海報");
        r.setBenefitType("REGULAR");
        r.setIsMemberOnly(Boolean.FALSE);
        r.setStartDate(LocalDateTime.of(2026, 10, 16, 0, 0));
        r.setEndDate(FAR_FUTURE);
        r.setSourceUrl("https://www.facebook.com/vieshow/posts/1");
        r.setCustomMovieName(customMovie);
        r.setMatchedCinemaIds(cinemaIds);
        r.setReviewNote("爬蟲自動建立，請確認電影與日期；電影庫找不到對應電影");
        return r;
    }

    @Test
    @DisplayName("匯入時沒上架的待審單，會記下比對到的影城ID，給之後重新比對用")
    void importStoresMatchedCinemaIds() {
        givenData();

        service.importAll(List.of(weekly("電影庫沒有的片", "海報")), true);

        ArgumentCaptor<BenefitReportBean> cap = ArgumentCaptor.forClass(BenefitReportBean.class);
        verify(reportRepo).save(cap.capture());
        assertEquals("1,2", cap.getValue().getMatchedCinemaIds());   // 秀泰不含「威秀」
        assertEquals("電影庫沒有的片", cap.getValue().getCustomMovieName());
    }

    @Test
    @DisplayName("重新比對：電影補齊且資料齊全 → 建立正式特典＋中介表，回報單改為已上架")
    void rematchApproves() {
        BenefitReportBean r = pendingNoMovie("惡靈古堡：爆發夜", "1,2");
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(r));
        givenData();
        givenBenefitSaveAssignsId(10);

        RematchSummary s = service.rematchPending();

        assertEquals(new RematchSummary(1, 1, 1, 0), s);
        assertEquals("APPROVED", r.getStatus());
        assertSame(resident, r.getMovie());
        assertNull(r.getCustomMovieName());
        assertEquals(10, r.getBenefit().getBenefitId());
        assertTrue(r.getReviewNote().contains("重新比對後自動上架"));
        ArgumentCaptor<BenefitBean> benefit = ArgumentCaptor.forClass(BenefitBean.class);
        verify(benefitRepo).save(benefit.capture());
        assertEquals("APPROVED", benefit.getValue().getStatus());
        assertEquals("REGULAR", benefit.getValue().getBenefitType());
        verify(linkRepo, times(2)).save(any(CinemaMovieBenefitBean.class));
        verify(reportRepo).save(r);
    }

    @Test
    @DisplayName("重新比對：電影還是找不到→不動這張單")
    void rematchStillNotFound() {
        BenefitReportBean r = pendingNoMovie("還是沒有的片", "1,2");
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(r));
        givenData();

        RematchSummary s = service.rematchPending();

        assertEquals(new RematchSummary(1, 0, 0, 1), s);
        assertEquals("PENDING", r.getStatus());
        assertNull(r.getMovie());
        verify(reportRepo, never()).save(any());
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("重新比對：舊資料沒記影城→只綁電影，仍待審核")
    void rematchWithoutCinemaIds() {
        BenefitReportBean r = pendingNoMovie("惡靈古堡：爆發夜", null);
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(r));
        givenData();

        RematchSummary s = service.rematchPending();

        assertEquals(new RematchSummary(1, 1, 0, 1), s);
        assertEquals("PENDING", r.getStatus());
        assertSame(resident, r.getMovie());
        assertTrue(r.getReviewNote().contains("已重新比對到電影"));
        verify(benefitRepo, never()).save(any());
        verify(linkRepo, never()).save(any());
        verify(reportRepo).save(r);
    }

    @Test
    @DisplayName("重新比對：缺開始日 → 只綁電影，仍待審核")
    void rematchIncomplete() {
        BenefitReportBean r = pendingNoMovie("惡靈古堡：爆發夜", "1,2");
        r.setStartDate(null);
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(r));
        givenData();

        RematchSummary s = service.rematchPending();

        assertEquals(new RematchSummary(1, 1, 0, 1), s);
        assertEquals("PENDING", r.getStatus());
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("重新比對：已經有同電影、同特典、同開始日的正式特典→不重複建立，留在待審核")
    void rematchDuplicate() {
        BenefitReportBean r = pendingNoMovie("惡靈古堡：爆發夜", "1,2");
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(r));
        givenData();
        when(benefitRepo.countByMovieAndNameAndStartDate(35, "厄夜之鼠款A3夜光海報", r.getStartDate())).thenReturn(1L);

        RematchSummary s = service.rematchPending();

        assertEquals(new RematchSummary(1, 1, 0, 1), s);
        assertEquals("PENDING", r.getStatus());
        assertTrue(r.getReviewNote().contains("已有相同特典"));
        verify(benefitRepo, never()).save(any());
    }

    @Test
    @DisplayName("重新比對：本來就綁好電影、或根本沒電影名的待審單不在檢查範圍")
    void rematchSkipsOthers() {
        BenefitReportBean bound = pendingNoMovie(null, "1,2");
        bound.setMovie(resident);
        BenefitReportBean noName = pendingNoMovie(null, "1,2");
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(bound, noName));
        givenData();

        RematchSummary s = service.rematchPending();

        assertEquals(new RematchSummary(0, 0, 0, 0), s);
        verify(reportRepo, never()).save(any());
    }

    @Test
    @DisplayName("重新比對：已關閉的影城不綁")
    void rematchIgnoresInactiveCinema() {
        BenefitReportBean r = pendingNoMovie("惡靈古堡：爆發夜", "1,99");
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(r));
        givenData();
        givenBenefitSaveAssignsId(10);

        service.rematchPending();

        ArgumentCaptor<CinemaMovieBenefitBean> link = ArgumentCaptor.forClass(CinemaMovieBenefitBean.class);
        verify(linkRepo).save(link.capture());
        assertEquals(1, link.getValue().getCinema().getCinemaId());
    }

    // ===== 圖片轉存 =====

    private ParsedBenefitPost withImage(ParsedBenefitPost p, String url) {
        return new ParsedBenefitPost(p.postId(), p.sourceUrl(), p.username(), p.text(), p.publishedAt(),
                p.movieName(), p.benefitName(), p.memberOnly(), p.benefitType(), p.startDate(), p.endDate(),
                List.of(url), p.reviewNote(), p.classification(), p.chainKeyword(), p.cinemaNames());
    }

    @Test
    @DisplayName("自動上架的特典：貼文第一張圖轉存到Cloudinary，網址與publicId存進特典")
    void benefitGetsImage() {
        givenData();
        givenBenefitSaveAssignsId(10);
        when(cloudinary.uploadFromUrl("https://fb/a.png"))
                .thenReturn(Optional.of(new UploadedImage("https://res.cloudinary.com/x/a.png", "benefits/a")));

        service.importAll(List.of(withImage(weekly("惡靈古堡：爆發夜", "海報"), "https://fb/a.png")), true);

        ArgumentCaptor<BenefitBean> cap = ArgumentCaptor.forClass(BenefitBean.class);
        verify(benefitRepo).save(cap.capture());
        assertEquals("https://res.cloudinary.com/x/a.png", cap.getValue().getOfficialImageUrl());
        assertEquals("benefits/a", cap.getValue().getPublicId());
    }

    @Test
    @DisplayName("待審核回報單也會帶圖，轉存失敗（空值）時照常建立，只是沒有圖")
    void reportImageAndFailure() {
        givenData();
        when(cloudinary.uploadFromUrl("https://fb/a.png")).thenReturn(Optional.empty());

        ImportSummary s = service.importAll(List.of(withImage(weekly("電影庫沒有的片", "海報"), "https://fb/a.png")), true);

        ArgumentCaptor<BenefitReportBean> cap = ArgumentCaptor.forClass(BenefitReportBean.class);
        verify(reportRepo).save(cap.capture());
        assertNull(cap.getValue().getOfficialImageUrl());
        assertEquals(new ImportSummary(1, 0, 0, 0), s);
    }

    @Test
    @DisplayName("同一篇貼文的兩筆特典只轉存一次圖片")
    void uploadOncePerPost() {
        givenData();
        givenBenefitSaveAssignsId(10);
        when(cloudinary.uploadFromUrl("https://fb/a.png"))
                .thenReturn(Optional.of(new UploadedImage("https://res.cloudinary.com/x/a.png", "benefits/a")));

        service.importAll(List.of(
                withImage(weekly("惡靈古堡：爆發夜", "海報A"), "https://fb/a.png"),
                withImage(weekly("惡靈古堡：爆發夜", "海報B"), "https://fb/a.png")), true);

        verify(cloudinary, times(1)).uploadFromUrl("https://fb/a.png");
        verify(benefitRepo, times(2)).save(any(BenefitBean.class));
    }

    @Test
    @DisplayName("沒有圖片網址：完全不呼叫Cloudinary")
    void noImageNoUpload() {
        givenData();
        givenBenefitSaveAssignsId(10);

        service.importAll(List.of(weekly("惡靈古堡：爆發夜", "海報")), true);

        verify(cloudinary, never()).uploadFromUrl(any());
    }

    @Test
    @DisplayName("重複匯入同一則貼文：之前沒圖的特典補上圖片，已經有圖的不動")
    void backfillMissingImage() {
        givenData();
        ParsedBenefitPost post = withImage(weekly("惡靈古堡：爆發夜", "海報"), "https://fb/a.png");
        when(benefitRepo.existsBySourceUrlAndBenefitName(post.sourceUrl(), "海報")).thenReturn(true);
        BenefitBean noImage = new BenefitBean();
        BenefitBean hasImage = new BenefitBean();
        hasImage.setOfficialImageUrl("https://keep");
        when(benefitRepo.findBySourceUrlAndBenefitName(post.sourceUrl(), "海報")).thenReturn(List.of(noImage, hasImage));
        when(cloudinary.uploadFromUrl("https://fb/a.png"))
                .thenReturn(Optional.of(new UploadedImage("https://res.cloudinary.com/x/a.png", "benefits/a")));

        ImportSummary s = service.importAll(List.of(post), true);

        assertEquals("https://res.cloudinary.com/x/a.png", noImage.getOfficialImageUrl());
        assertEquals("benefits/a", noImage.getPublicId());
        assertEquals("https://keep", hasImage.getOfficialImageUrl());
        verify(benefitRepo).save(noImage);
        verify(benefitRepo, never()).save(hasImage);
        assertEquals(new ImportSummary(0, 0, 1, 0), s);
    }

    @Test
    @DisplayName("重複匯入、特典與回報單都已經有圖：不呼叫 Cloudinary")
    void backfillSkippedWhenAllHaveImage() {
        givenData();
        ParsedBenefitPost post = withImage(weekly("惡靈古堡：爆發夜", "海報"), "https://fb/a.png");
        when(benefitRepo.existsBySourceUrlAndBenefitName(post.sourceUrl(), "海報")).thenReturn(true);
        BenefitBean hasImage = new BenefitBean();
        hasImage.setOfficialImageUrl("https://keep");
        when(benefitRepo.findBySourceUrlAndBenefitName(post.sourceUrl(), "海報")).thenReturn(List.of(hasImage));

        service.importAll(List.of(post), true);

        verify(cloudinary, never()).uploadFromUrl(any());
    }

    @Test
    @DisplayName("重新比對上架時，回報單上的圖片一起帶到正式特典")
    void rematchCarriesImage() {
        BenefitReportBean r = pendingNoMovie("惡靈古堡：爆發夜", "1,2");
        r.setOfficialImageUrl("https://res.cloudinary.com/x/a.png");
        r.setPublicId("benefits/a");
        when(reportRepo.findByStatusAndUserIsNull("PENDING")).thenReturn(List.of(r));
        givenData();
        givenBenefitSaveAssignsId(10);

        service.rematchPending();

        ArgumentCaptor<BenefitBean> cap = ArgumentCaptor.forClass(BenefitBean.class);
        verify(benefitRepo).save(cap.capture());
        assertEquals("https://res.cloudinary.com/x/a.png", cap.getValue().getOfficialImageUrl());
        assertEquals("benefits/a", cap.getValue().getPublicId());
    }

    @Test
    @DisplayName("圖片轉存連續失敗3次：這次匯入不再嘗試，其餘特典照常建立")
    void stopUploadingAfterRepeatedFailures() {
        givenData();
        givenBenefitSaveAssignsId(10);
        when(cloudinary.uploadFromUrl(any())).thenReturn(Optional.empty());

        List<ParsedBenefitPost> posts = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ParsedBenefitPost p = weekly("惡靈古堡：爆發夜", "海報" + i);
            posts.add(new ParsedBenefitPost(p.postId(), "https://www.facebook.com/vieshow/posts/" + (100 + i),
                    p.username(), p.text(), p.publishedAt(), p.movieName(), p.benefitName(), p.memberOnly(),
                    p.benefitType(), p.startDate(), p.endDate(), List.of("https://fb/" + i + ".png"),
                    p.reviewNote(), p.classification(), p.chainKeyword(), p.cinemaNames()));
        }

        ImportSummary s = service.importAll(posts, true);

        verify(cloudinary, times(3)).uploadFromUrl(any());
        assertEquals(new ImportSummary(5, 5, 0, 0), s);
    }
}