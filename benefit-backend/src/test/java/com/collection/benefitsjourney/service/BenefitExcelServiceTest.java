package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
class BenefitExcelServiceTest {

    @Mock BenefitRepository benefitRepo;
    @Mock CinemaMovieBenefitRepository linkRepo;
    @Mock BenefitReportRepository reportRepo;
    @InjectMocks BenefitExcelService service;

    @Test
    @DisplayName("三張工作表：特典、特典_影城、回報單（預設只放待審核）")
    void export() throws Exception {
        BenefitBean b = new BenefitBean();
        b.setBenefitId(1);
        b.setBenefitName("=惡意公式");
        b.setBenefitType("REGULAR");
        b.setStatus("APPROVED");
        b.setStartDate(LocalDateTime.of(2026, 10, 16, 0, 0));
        MovieBean m = new MovieBean();
        m.setMovieId(5);
        m.setMovieName("惡靈古堡：爆發夜");
        CinemaBean c = new CinemaBean();
        c.setCinemaId(2);
        c.setCinemaName("台北信義威秀");
        CinemaMovieBenefitBean link = new CinemaMovieBenefitBean();
        link.setCinemaMovieBenefitId(1);
        link.setBenefit(b);
        link.setMovie(m);
        link.setCinema(c);
        link.setBenefitInventoryStatus(1);

        BenefitReportBean pending = report(10, "PENDING");
        BenefitReportBean done = report(11, "APPROVED");

        when(benefitRepo.findAll()).thenReturn(List.of(b));
        when(linkRepo.findAll()).thenReturn(List.of(link));
        when(reportRepo.findAll()).thenReturn(List.of(pending, done));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.export(true)))) {
            assertEquals(3, wb.getNumberOfSheets());
            Sheet s1 = wb.getSheet("特典");
            assertEquals(2, s1.getPhysicalNumberOfRows());
            assertEquals("'=惡意公式", s1.getRow(1).getCell(1).getStringCellValue());
            assertEquals("惡靈古堡：爆發夜", s1.getRow(1).getCell(7).getStringCellValue());
            assertEquals(1, (int) s1.getRow(1).getCell(8).getNumericCellValue());
            assertEquals("IN_STOCK", wb.getSheet("特典_影城").getRow(1).getCell(6).getStringCellValue());
            assertEquals(2, wb.getSheet("回報單").getPhysicalNumberOfRows());   // 表頭＋1 筆 PENDING
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.export(false)))) {
            assertEquals(3, wb.getSheet("回報單").getPhysicalNumberOfRows());   // 表頭＋2 筆
        }
    }

    private BenefitReportBean report(int id, String status) {
        BenefitReportBean r = new BenefitReportBean();
        r.setBenefitReportId(id);
        r.setStatus(status);
        r.setBenefitName("海報");
        r.setCustomMovieName("泥面人");
        return r;
    }
}