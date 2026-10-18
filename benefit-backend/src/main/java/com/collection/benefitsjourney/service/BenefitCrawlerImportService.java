package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.BenefitReportBean;
import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.enums.BenefitReportStatus;
import com.collection.benefitsjourney.enums.BenefitStatus;
import com.collection.benefitsjourney.repository.BenefitReportRepository;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.service.CloudinaryClient.UploadedImage;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;

/**
 * 把Parser解析好的貼文寫進資料庫。
 * 爬蟲資料齊全、電影與影城都比對成功→直接建立正式特典（Benefit）＋中介表（CinemaMovieBenefit），不寫審核回報。
 * 解析不完整或比對失敗→只寫一筆待審核的回報單（BenefitReport，user=null），review_note寫原因。
 * 另外負責略過非特典、去重（同貼文同特典、同電影同特典同起始日）。
 * autoApprove=false時，所有結果都只寫成待審核回報單。
 */
@Slf4j
@Service
public class BenefitCrawlerImportService {

    private final BenefitReportRepository benefitReportRepository;
    private final BenefitRepository benefitRepository;
    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;
    private final MovieRepository movieRepository;
    private final CinemaRepository cinemaRepository;
    private final CloudinaryClient cloudinaryClient;

    public BenefitCrawlerImportService(BenefitReportRepository benefitReportRepository,
                                       BenefitRepository benefitRepository,
                                       CinemaMovieBenefitRepository cinemaMovieBenefitRepository,
                                       MovieRepository movieRepository,
                                       CinemaRepository cinemaRepository,
                                       CloudinaryClient cloudinaryClient) {
        this.benefitReportRepository = benefitReportRepository;
        this.benefitRepository = benefitRepository;
        this.cinemaMovieBenefitRepository = cinemaMovieBenefitRepository;
        this.movieRepository = movieRepository;
        this.cinemaRepository = cinemaRepository;
        this.cloudinaryClient = cloudinaryClient;
    }

    /**
     * 這次匯入的結果統計：
     * created＝新增的筆數（自動上架的正式特典＋待審核回報單）
     * autoApproved＝其中直接上架的正式特典（所以待審核回報單＝created−autoApproved）
     */
    public record ImportSummary(int created, int autoApproved, int duplicated, int ignored) {
    }

    /**
     * 「重新比對」的結果統計：
     * checked＝檢查了幾張「電影庫找不到對應電影」的待審單、movieMatched＝這次比對到電影的張數
     * approved＝其中資料齊全、直接上架的張數、stillPending＝仍留在待審核的張數（＝checked−approved）
     */
    public record RematchSummary(int checked, int movieMatched, int approved, int stillPending) {
    }

    @Transactional
    public ImportSummary importAll(List<ParsedBenefitPost> posts, boolean autoApprove) {
        List<MovieBean> movies = movieRepository.findAll();
        List<String> movieNames = movies.stream().map(MovieBean::getMovieName).toList();
        List<CinemaBean> cinemas = cinemaRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList();
        List<String> cinemaNames = cinemas.stream().map(CinemaBean::getCinemaName).toList();

        int created = 0;
        int approved = 0;
        int duplicated = 0;
        int ignored = 0;
        Map<String, Optional<UploadedImage>> imageCache = new HashMap<>();   // 同一篇貼文的多筆特典只轉存一次
        imageFailures = 0;

        for (ParsedBenefitPost post : posts) {
            if (post.classification() == Classification.NOT_BENEFIT || post.sourceUrl() == null) {
                ignored++;
                continue;
            }
            if (isDuplicate(post)) {
                backfillImage(post, imageCache);
                duplicated++;
                continue;
            }

            BenefitReportBean report = toReport(post);
            int movieIndex = MovieNameMatcher.findIndex(post.movieName(), movieNames);
            StringBuilder note = new StringBuilder(post.reviewNote() == null ? "" : post.reviewNote());
            MovieBean movie = null;
            if (movieIndex >= 0) {
                movie = movies.get(movieIndex);
                report.setMovie(movie);
            } else if (post.movieName() != null) {
                report.setCustomMovieName(limit(post.movieName(), 100));
                note.append("；電影庫找不到對應電影");
            } else {
                note.append("；貼文沒有電影名");
            }

            // 同一部電影、同特典、同起始日已經有特典或回報單（不同貼文重複宣傳同一個特典）
            if (movie != null && post.benefitName() != null && post.startDate() != null
                    && isSameBenefitExists(movie.getMovieId(), post.benefitName(), post.startDate())) {
                duplicated++;
                continue;
            }

            List<Integer> cinemaIds = resolveCinemas(post, cinemas, cinemaNames, note);

            if (autoApprove && canAutoApprove(post, movie, cinemaIds)) {
                createApprovedBenefit(post, movie, cinemaIds, cinemas, limit(approvedNote(note.toString()), 100),
                        imageOf(post, imageCache));
                created++;
                approved++;
                continue;
            }

            report.setReviewNote(limit(note.toString(), 100));
            report.setMatchedCinemaIds(joinIds(cinemaIds));   // 之後「重新比對」要靠它知道這則特典在哪些影城
            imageOf(post, imageCache).ifPresent(img -> {
                report.setOfficialImageUrl(img.url());
                report.setPublicId(img.publicId());
            });
            benefitReportRepository.save(report);
            created++;
        }
        return new ImportSummary(created, approved, duplicated, ignored);
    }

    /** 有特典名：同貼文＋同特典名算重複（正式特典或回報單任一已存在）／沒特典名：同貼文已有回報單就算重複 */
    private boolean isDuplicate(ParsedBenefitPost post) {
        if (post.benefitName() == null) {
            return benefitReportRepository.existsBySourceUrl(limit(post.sourceUrl(), 300));
        }
        String name = limit(post.benefitName(), 100);       // 資料庫存的是截斷後的名字，比對也要用同樣長度
        String url = limit(post.sourceUrl(), 300);
        return benefitReportRepository.existsBySourceUrlAndBenefitName(url, name)
                || benefitRepository.existsBySourceUrlAndBenefitName(url, name);
    }

    private boolean isSameBenefitExists(Integer movieId, String benefitName, LocalDateTime startDate) {
        String name = limit(benefitName, 100);
        return benefitReportRepository.existsByMovie_MovieIdAndBenefitNameAndStartDate(movieId, name, startDate)
                || benefitRepository.countByMovieAndNameAndStartDate(movieId, name, startDate) > 0;
    }

    /** 找出這則特典適用的影城ID：全連鎖用關鍵字，特別場用貼文列出的影城簡稱 */
    private List<Integer> resolveCinemas(ParsedBenefitPost post, List<CinemaBean> cinemas,
                                         List<String> cinemaNames, StringBuilder note) {
        List<Integer> ids = new ArrayList<>();
        if (post.chainKeyword() != null) {
            for (CinemaBean c : cinemas) {
                if (c.getCinemaName() != null && c.getCinemaName().contains(post.chainKeyword())) {
                    ids.add(c.getCinemaId());
                }
            }
            return ids;
        }
        CinemaNameMatcher.Match m = CinemaNameMatcher.match(post.cinemaNames(), cinemaNames);
        for (int i : m.indexes()) {
            ids.add(cinemas.get(i).getCinemaId());
        }
        if (!m.unresolved().isEmpty()) {
            note.append("；未對應影城：").append(String.join("、", m.unresolved()));
        }
        return ids;
    }

    /** 資料完整（BENEFIT）、電影對得上、至少一間影城對得上才自動上架（對不上的影城名會寫在備註） */
    private boolean canAutoApprove(ParsedBenefitPost post, MovieBean movie, List<Integer> cinemaIds) {
        return post.classification() == Classification.BENEFIT
                && movie != null
                && !cinemaIds.isEmpty()
                && post.benefitName() != null
                && post.startDate() != null
                && post.benefitType() != null;
    }

    /** 直接建立正式特典＋中介表（做法和管理員核准回報單一樣：同電影已有同名上架特典就沿用） */
    private void createApprovedBenefit(ParsedBenefitPost post, MovieBean movie, List<Integer> cinemaIds,
                                       List<CinemaBean> cinemas, String note, Optional<UploadedImage> image) {
        String name = limit(post.benefitName(), 100);
        publish(movie, name, cinemaIds, cinemas, () -> newBenefit(name, post.memberOnly(), post.benefitType(),
                post.startDate(), post.endDate(), post.sourceUrl(), note,
                image.map(UploadedImage::url).orElse(null), image.map(UploadedImage::publicId).orElse(null)));
    }

    /** 這次匯入已經連續失敗幾張圖，連續失敗3次就不再嘗試（網路問題或網址過期時不會每篇都等到逾時） */
    private int imageFailures;
    private static final int MAX_IMAGE_FAILURES = 3;

    /** 貼文的第一張圖轉存到Cloudinary，同一篇貼文只轉存一次（失敗也記下來，不重試）。沒圖、沒設定、失敗都回空 */
    private Optional<UploadedImage> imageOf(ParsedBenefitPost post, Map<String, Optional<UploadedImage>> cache) {
        if (post.imageUrls() == null || post.imageUrls().isEmpty()) {
            return Optional.empty();
        }
        Optional<UploadedImage> cached = cache.get(post.sourceUrl());
        if (cached != null) {
            return cached;
        }
        if (imageFailures >= MAX_IMAGE_FAILURES) {
            return Optional.empty();                      // 先前連續失敗太多次，這次匯入不再嘗試
        }
        Optional<UploadedImage> result = cloudinaryClient.uploadFromUrl(post.imageUrls().get(0));
        if (result == null) {
            result = Optional.empty();
        }
        imageFailures = result.isPresent() ? 0 : imageFailures + 1;
        if (imageFailures == MAX_IMAGE_FAILURES) {
            log.warn("圖片轉存連續失敗 {} 次，這次匯入不再嘗試轉存（可先用 GET /crawler/benefits/cloudinary-check檢查連線，修好後重新匯入同一份資料即可補圖）", MAX_IMAGE_FAILURES);
        }
        cache.put(post.sourceUrl(), result);
        return result;
    }

    /**
     * 重複的貼文：如果之前匯入時沒有圖（例如當時還沒做圖片轉存），可以補上。
     * 只補「圖片欄位是空的」的特典與回報單，已經有圖的不動。
     */
    private void backfillImage(ParsedBenefitPost post, Map<String, Optional<UploadedImage>> cache) {
        if (post.imageUrls() == null || post.imageUrls().isEmpty()) {
            return;
        }
        String url = limit(post.sourceUrl(), 300);
        List<BenefitBean> benefits = post.benefitName() == null ? List.of()
                : benefitRepository.findBySourceUrlAndBenefitName(url, limit(post.benefitName(), 100));
        List<BenefitReportBean> reports = post.benefitName() == null
                ? benefitReportRepository.findBySourceUrl(url)
                : benefitReportRepository.findBySourceUrlAndBenefitName(url, limit(post.benefitName(), 100));

        boolean needBenefit = benefits.stream().anyMatch(b -> isBlank(b.getOfficialImageUrl()));
        boolean needReport = reports.stream().anyMatch(r -> isBlank(r.getOfficialImageUrl()));
        if (!needBenefit && !needReport) {
            return;
        }
        imageOf(post, cache).ifPresent(img -> {
            for (BenefitBean b : benefits) {
                if (isBlank(b.getOfficialImageUrl())) {
                    b.setOfficialImageUrl(img.url());
                    b.setPublicId(img.publicId());
                    benefitRepository.save(b);
                }
            }
            for (BenefitReportBean r : reports) {
                if (isBlank(r.getOfficialImageUrl())) {
                    r.setOfficialImageUrl(img.url());
                    r.setPublicId(img.publicId());
                    benefitReportRepository.save(r);
                }
            }
        });
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 沿用（或用creator新建）正式特典，再替每間影城補中介表，回傳這筆正式特典 */
    private BenefitBean publish(MovieBean movie, String name, List<Integer> cinemaIds, List<CinemaBean> cinemas,
                                Supplier<BenefitBean> creator) {
        BenefitBean benefit = benefitRepository.findApprovedByMovieAndName(movie.getMovieId(), name)
                .stream().findFirst().orElseGet(() -> benefitRepository.save(creator.get()));

        for (CinemaBean cinema : cinemas) {
            if (!cinemaIds.contains(cinema.getCinemaId())) {
                continue;
            }
            if (cinemaMovieBenefitRepository.existsByCinema_CinemaIdAndMovie_MovieIdAndBenefit_BenefitId(
                    cinema.getCinemaId(), movie.getMovieId(), benefit.getBenefitId())) {
                continue;
            }
            CinemaMovieBenefitBean link = new CinemaMovieBenefitBean();
            link.setCinema(cinema);
            link.setMovie(movie);
            link.setBenefit(benefit);
            cinemaMovieBenefitRepository.save(link);
        }
        return benefit;
    }

    private BenefitBean newBenefit(String name, Boolean memberOnly, String type, LocalDateTime start,
                                   LocalDateTime end, String sourceUrl, String note,
                                   String imageUrl, String publicId) {
        BenefitBean benefit = new BenefitBean();
        benefit.setBenefitName(name);
        benefit.setIsMemberOnly(memberOnly);
        benefit.setBenefitType(type);
        benefit.setStartDate(start);
        benefit.setEndDate(end);
        benefit.setSourceUrl(limit(sourceUrl, 300));
        benefit.setReviewNote(note);
        benefit.setOfficialImageUrl(imageUrl);
        benefit.setPublicId(publicId);
        // 結束日已經過了就直接設為已下架
        boolean ended = end != null && end.isBefore(LocalDateTime.now());
        benefit.setStatus((ended ? BenefitStatus.EXPIRED : BenefitStatus.APPROVED).name());
        return benefit;
    }

    /**
     * 重新比對：電影表補齊後，把「電影庫找不到對應電影」的爬蟲待審單再比對一次。
     * 電影比對到了→回報單改綁這部電影。
     * 而且贈品名、開始日、類型、影城都齊全→直接建立正式特典＋中介表，回報單標為已上架。
     * 還缺資料→維持待審核（只更新電影），備註改成「已重新比對到電影」。
     * 影城是匯入當時就記在回報單上的（matchedCinemaIds）；舊資料沒有記的，只會綁電影，要人工補影城再核准。
     */
    @Transactional
    public RematchSummary rematchPending() {
        List<BenefitReportBean> pending =
                benefitReportRepository.findByStatusAndUserIsNull(BenefitReportStatus.PENDING.name());
        List<MovieBean> movies = movieRepository.findAll();
        List<String> movieNames = movies.stream().map(MovieBean::getMovieName).toList();
        List<CinemaBean> cinemas = cinemaRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList();

        int checked = 0;
        int matched = 0;
        int approved = 0;
        for (BenefitReportBean r : pending) {
            if (r.getMovie() != null || r.getCustomMovieName() == null || r.getCustomMovieName().isBlank()) {
                continue;                                   // 沒有電影名可比，或早就綁好電影了
            }
            checked++;
            int index = MovieNameMatcher.findIndex(r.getCustomMovieName(), movieNames);
            if (index < 0) {
                continue;
            }
            matched++;
            MovieBean movie = movies.get(index);
            r.setMovie(movie);
            r.setCustomMovieName(null);

            List<Integer> cinemaIds = parseIds(r.getMatchedCinemaIds(), cinemas);
            boolean complete = r.getBenefitName() != null && !r.getBenefitName().isBlank()
                    && r.getStartDate() != null && r.getBenefitType() != null && !cinemaIds.isEmpty();
            String base = r.getReviewNote() == null ? "" : r.getReviewNote();

            if (!complete) {
                r.setReviewNote(limit(replaceNotFound(base, "已重新比對到電影"), 100));
            } else if (benefitRepository.countByMovieAndNameAndStartDate(
                    movie.getMovieId(), r.getBenefitName(), r.getStartDate()) > 0) {
                r.setReviewNote(limit(replaceNotFound(base, "已重新比對到電影，且已有相同特典，請確認是否駁回"), 100));
            } else {
                String note = limit(approvedNote(replaceNotFound(base, "重新比對後自動上架")), 100);
                BenefitBean benefit = publish(movie, r.getBenefitName(), cinemaIds, cinemas,
                        () -> newBenefit(r.getBenefitName(), r.getIsMemberOnly(), r.getBenefitType(),
                                r.getStartDate(), r.getEndDate(), r.getSourceUrl(), note,
                                r.getOfficialImageUrl(), r.getPublicId()));
                r.setStatus(BenefitReportStatus.APPROVED.name());
                r.setBenefit(benefit);
                r.setReviewNote(note);
                approved++;
            }
            benefitReportRepository.save(r);
        }
        return new RematchSummary(checked, matched, approved, checked - approved);
    }

    /** 已經上架的特典不需要再「請確認電影與日期」，把這句拿掉（待審核的回報單仍保留這句提醒） */
    private String approvedNote(String note) {
        return note.replace("爬蟲自動建立，請確認電影與日期", "爬蟲自動建立");
    }

    /** 備註裡的「電影庫找不到對應電影」換成新說明，原本沒有這句就接在後面 */
    private String replaceNotFound(String note, String replacement) {
        String key = "電影庫找不到對應電影";
        if (note.contains(key)) {
            return note.replace(key, replacement);
        }
        return note.isEmpty() ? replacement : note + "；" + replacement;
    }

    private String joinIds(List<Integer> ids) {
        if (ids.isEmpty()) {
            return null;
        }
        return limit(String.join(",", ids.stream().map(String::valueOf).toList()), 600);
    }

    /** 把"1,2,3"轉回影城ID，只留下目前還營業中的影城 */
    private List<Integer> parseIds(String text, List<CinemaBean> activeCinemas) {
        List<Integer> ids = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return ids;
        }
        Set<Integer> active = new java.util.HashSet<>();
        activeCinemas.forEach(c -> active.add(c.getCinemaId()));
        for (String s : text.split(",")) {
            try {
                int id = Integer.parseInt(s.trim());
                if (active.contains(id)) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignore) {
                // 壞掉的片段直接略過
            }
        }
        return ids;
    }

    private BenefitReportBean toReport(ParsedBenefitPost post) {
        BenefitReportBean report = new BenefitReportBean();
        report.setStatus(BenefitReportStatus.PENDING.name());
        report.setBenefitName(limit(post.benefitName(), 100));
        report.setIsMemberOnly(post.memberOnly());
        report.setBenefitType(post.benefitType());
        report.setStartDate(post.startDate());
        report.setEndDate(post.endDate());
        report.setSourceUrl(limit(post.sourceUrl(), 300));
        report.setCustomCinemaName(chainName(post.username()));
        return report;
    }

    private String chainName(String username) {
        if (username != null && username.toLowerCase().contains("vieshow")) {
            return "威秀影城";
        }
        return limit(username, 100);
    }

    private String limit(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}