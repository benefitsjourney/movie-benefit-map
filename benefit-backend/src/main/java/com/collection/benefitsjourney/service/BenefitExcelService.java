package com.collection.benefitsjourney.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.BenefitReportBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitReportRepository;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;

import lombok.RequiredArgsConstructor;

/**
 * 特典Excel匯出
 * 一個檔案三張工作表
 * 1. 特典		正式特典（含適用電影、影城數量）
 * 2. 特典_影城	中介表（含庫存狀態）
 * 3. 回報單		爬蟲／使用者回報單（預設只放待審核PENDING，方便沒有前端時一筆筆對照審核）
 */
@Service
@RequiredArgsConstructor
public class BenefitExcelService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BenefitRepository benefitRepository;
    private final CinemaMovieBenefitRepository linkRepository;
    private final BenefitReportRepository reportRepository;

    /**
     * @param onlyPendingReports true＝回報單工作表只放PENDING，false＝全部狀態
     */
    @Transactional(readOnly = true)
    public byte[] export(boolean onlyPendingReports) {
        List<BenefitBean> benefits = benefitRepository.findAll().stream()
                .sorted(Comparator.comparing(BenefitBean::getBenefitId)).toList();
        List<CinemaMovieBenefitBean> links = linkRepository.findAll().stream()
                .sorted(Comparator.comparing(CinemaMovieBenefitBean::getCinemaMovieBenefitId)).toList();
        List<BenefitReportBean> reports = reportRepository.findAll().stream()
                .filter(r -> !onlyPendingReports || "PENDING".equals(r.getStatus()))
                .sorted(Comparator.comparing(BenefitReportBean::getBenefitReportId)).toList();

        // 每個特典對應到哪些電影、幾間影城
        Map<Integer, Set<String>> movieNames = new LinkedHashMap<>();
        Map<Integer, Integer> cinemaCount = new LinkedHashMap<>();
        for (CinemaMovieBenefitBean l : links) {
            Integer bid = l.getBenefit().getBenefitId();
            movieNames.computeIfAbsent(bid, k -> new LinkedHashSet<>()).add(l.getMovie().getMovieName());
            cinemaCount.merge(bid, 1, Integer::sum);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = headerStyle(wb);

            Sheet s1 = wb.createSheet("特典");
            header(s1, head, "benefitId", "特典名稱", "類型", "狀態", "開始", "結束", "會員限定",
                    "適用電影", "影城數", "來源貼文", "備註");
            int r = 1;
            for (BenefitBean b : benefits) {
                write(s1.createRow(r++),
                        b.getBenefitId(), b.getBenefitName(), b.getBenefitType(), b.getStatus(),
                        dt(b.getStartDate()), dt(b.getEndDate()), b.getIsMemberOnly(),
                        String.join("、", movieNames.getOrDefault(b.getBenefitId(), Set.of())),
                        cinemaCount.getOrDefault(b.getBenefitId(), 0),
                        b.getSourceUrl(), b.getReviewNote());
            }

            Sheet s2 = wb.createSheet("特典_影城");
            header(s2, head, "benefitId", "特典名稱", "movieId", "電影", "cinemaId", "影城", "庫存狀態");
            r = 1;
            for (CinemaMovieBenefitBean l : links) {
                write(s2.createRow(r++),
                        l.getBenefit().getBenefitId(), l.getBenefit().getBenefitName(),
                        l.getMovie().getMovieId(), l.getMovie().getMovieName(),
                        l.getCinema().getCinemaId(), l.getCinema().getCinemaName(),
                        BenefitInventoryStatus.fromCode(l.getBenefitInventoryStatus()).name());
            }

            Sheet s3 = wb.createSheet("回報單");
            header(s3, head, "reportId", "狀態", "特典名稱", "電影", "電影庫找不到的片名", "影城",
                    "類型", "開始", "結束", "來源貼文", "審核備註");
            r = 1;
            for (BenefitReportBean p : reports) {
                write(s3.createRow(r++),
                        p.getBenefitReportId(), p.getStatus(), p.getBenefitName(),
                        p.getMovie() != null ? p.getMovie().getMovieName() : null,
                        p.getCustomMovieName(),
                        p.getCinema() != null ? p.getCinema().getCinemaName() : p.getCustomCinemaName(),
                        p.getBenefitType(), dt(p.getStartDate()), dt(p.getEndDate()),
                        p.getSourceUrl(), p.getReviewNote());
            }

            for (Sheet s : List.of(s1, s2, s3)) {
                s.createFreezePane(0, 1);
                for (int c = 0; c < 11; c++) {
                    s.setColumnWidth(c, 18 * 256);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw ExceptionUtils.badRequest("Excel匯出失敗：" + e.getMessage());
        }
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        Font f = wb.createFont();
        f.setBold(true);
        CellStyle s = wb.createCellStyle();
        s.setFont(f);
        return s;
    }

    private void header(Sheet sheet, CellStyle style, String... names) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < names.length; i++) {
            row.createCell(i).setCellValue(names[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private void write(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            Object v = values[i];
            if (v == null) {
                row.createCell(i).setBlank();
            } else if (v instanceof Number n) {
                row.createCell(i).setCellValue(n.doubleValue());
            } else {
                row.createCell(i).setCellValue(safe(v.toString()));
            }
        }
    }

    private String dt(LocalDateTime d) {
        return d == null ? null : d.format(DT);
    }

    /** 避免Excel公式注入（按照ExcelService的做法） */
    private String safe(String text) {
        return !text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0 ? "'" + text : text;
    }
}