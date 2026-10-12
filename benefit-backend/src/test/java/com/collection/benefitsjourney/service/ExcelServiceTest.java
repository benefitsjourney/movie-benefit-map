package com.collection.benefitsjourney.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaSort;
import com.collection.benefitsjourney.enums.ExportType;
import com.collection.benefitsjourney.enums.MovieSort;
import com.collection.benefitsjourney.enums.SortOrder;

import tools.jackson.databind.json.JsonMapper;

/**
 * 不啟動 Spring Context、不連線 SQL Server。
 * 使用真正的 JsonMapper 和 Apache POI，驗證產出的 .xlsx 內容。
 * CinemaService、MovieService 使用 Mockito 模擬分頁查詢。
 */
@ExtendWith(MockitoExtension.class)
class ExcelServiceTest {

    private static final int PAGE_SIZE = 200;

    @Mock
    private CinemaService cinemaService;

    @Mock
    private MovieService movieService;

    private ExcelService excelService;

    // 測試資料結構：不綁定 CinemaResponse、MovieResponse 的建構子欄位順序。
    public record CinemaRow(int cinemaId, String cinemaName, boolean isVisible) {
    }

    public record MovieRow(int movieId, String movieName) {
    }

    public record TextRow(String value) {
    }

    public record MixedRow(Integer id, String name, Boolean active, String note) {
    }

    @BeforeEach
    void setUp() {
        excelService = new ExcelService(
                cinemaService,
                movieService,
                JsonMapper.builder().build());
    }

    /** 模擬 PageResponse；同時相容 totalElements() 為 int 或 long 的版本。 */
    private PageResponse<?> page(List<?> items, int totalElements) {
        PageResponse<?> result = mock(PageResponse.class);
        doReturn(items).when(result).content();

        // ExcelService 遇到空清單後會直接 break，避免產生未使用的 Stub。
        if (!items.isEmpty()) {
            doAnswer(invocation -> {
                Class<?> returnType = invocation.getMethod().getReturnType();
                if (returnType == long.class || returnType == Long.class) {
                    return (long) totalElements;
                }
                return totalElements;
            }).when(result).totalElements();
        }
        return result;
    }

    private void stubCinemaPage(int page, PageResponse<?> response) {
        doReturn(response).when(cinemaService).findAll(
                isNull(), isNull(), isNull(), isNull(),
                eq(page), eq(PAGE_SIZE),
                eq(CinemaSort.SortBy.CINEMA_ID), eq(SortOrder.ASC));
    }

    private void stubMoviePage(int page, PageResponse<?> response) {
        doReturn(response).when(movieService).findAll(
                isNull(), isNull(),
                eq(page), eq(PAGE_SIZE),
                eq(MovieSort.SortBy.MOVIE_ID), eq(SortOrder.ASC));
    }

    private XSSFWorkbook readWorkbook(byte[] bytes) throws IOException {
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    @Nested
    @DisplayName("CINEMA - 匯出影城")
    class CinemaExportTests {

        @Test
        @DisplayName("正確建立工作表、標題列與影城資料")
        void exportCinema() throws Exception {
            stubCinemaPage(1, page(List.of(
                    new CinemaRow(100, "信義威秀", true),
                    new CinemaRow(101, "京站威秀", false)), 2));

            byte[] result = excelService.export(ExportType.CINEMA);

            try (XSSFWorkbook workbook = readWorkbook(result)) {
                assertEquals(1, workbook.getNumberOfSheets());
                Sheet sheet = workbook.getSheet("CINEMA");
                assertNotNull(sheet);
                assertEquals(3, sheet.getPhysicalNumberOfRows());

                assertEquals("cinemaId", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("cinemaName", sheet.getRow(0).getCell(1).getStringCellValue());
                assertEquals("isVisible", sheet.getRow(0).getCell(2).getStringCellValue());

                assertEquals("100", sheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("信義威秀", sheet.getRow(1).getCell(1).getStringCellValue());
                assertEquals("true", sheet.getRow(1).getCell(2).getStringCellValue());

                assertEquals("101", sheet.getRow(2).getCell(0).getStringCellValue());
                assertEquals("京站威秀", sheet.getRow(2).getCell(1).getStringCellValue());
                assertEquals("false", sheet.getRow(2).getCell(2).getStringCellValue());
            }

            verify(cinemaService).findAll(
                    null, null, null, null, 1, PAGE_SIZE,
                    CinemaSort.SortBy.CINEMA_ID, SortOrder.ASC);
            verifyNoInteractions(movieService);
        }

        @Test
        @DisplayName("沒有影城資料時仍能產生合法的空白活頁簿")
        void emptyCinema() throws Exception {
            stubCinemaPage(1, page(List.of(), 0));

            byte[] result = excelService.export(ExportType.CINEMA);

            try (XSSFWorkbook workbook = readWorkbook(result)) {
                Sheet sheet = workbook.getSheet("CINEMA");
                assertNotNull(sheet);
                assertEquals(0, sheet.getPhysicalNumberOfRows());
            }

            verify(cinemaService).findAll(
                    null, null, null, null, 1, PAGE_SIZE,
                    CinemaSort.SortBy.CINEMA_ID, SortOrder.ASC);
            verifyNoInteractions(movieService);
        }
    }

    @Nested
    @DisplayName("MOVIE - 匯出電影")
    class MovieExportTests {

        @Test
        @DisplayName("正確呼叫 MovieService，匯出電影欄位與資料")
        void exportMovie() throws Exception {
            stubMoviePage(1, page(List.of(new MovieRow(1, "測試電影")), 1));

            byte[] result = excelService.export(ExportType.MOVIE);

            try (XSSFWorkbook workbook = readWorkbook(result)) {
                Sheet sheet = workbook.getSheet("MOVIE");
                assertNotNull(sheet);
                assertEquals(2, sheet.getPhysicalNumberOfRows());
                assertEquals("movieId", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("movieName", sheet.getRow(0).getCell(1).getStringCellValue());
                assertEquals("1", sheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("測試電影", sheet.getRow(1).getCell(1).getStringCellValue());
            }

            verify(movieService).findAll(
                    null, null, 1, PAGE_SIZE,
                    MovieSort.SortBy.MOVIE_ID, SortOrder.ASC);
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("超過 200 筆會繼續查下一頁，且標題列只建立一次")
        void exportMultiplePages() throws Exception {
            List<MovieRow> firstPage = IntStream.rangeClosed(1, 200)
                    .mapToObj(i -> new MovieRow(i, "電影" + i))
                    .toList();

            stubMoviePage(1, page(firstPage, 201));
            stubMoviePage(2, page(List.of(new MovieRow(201, "電影201")), 201));

            byte[] result = excelService.export(ExportType.MOVIE);

            try (XSSFWorkbook workbook = readWorkbook(result)) {
                Sheet sheet = workbook.getSheet("MOVIE");
                assertNotNull(sheet);
                assertEquals(202, sheet.getPhysicalNumberOfRows());
                assertEquals("movieId", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("1", sheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("200", sheet.getRow(200).getCell(0).getStringCellValue());
                assertEquals("201", sheet.getRow(201).getCell(0).getStringCellValue());
                assertEquals("電影201", sheet.getRow(201).getCell(1).getStringCellValue());
            }

            verify(movieService).findAll(
                    null, null, 1, PAGE_SIZE,
                    MovieSort.SortBy.MOVIE_ID, SortOrder.ASC);
            verify(movieService).findAll(
                    null, null, 2, PAGE_SIZE,
                    MovieSort.SortBy.MOVIE_ID, SortOrder.ASC);
            verify(movieService, never()).findAll(
                    null, null, 3, PAGE_SIZE,
                    MovieSort.SortBy.MOVIE_ID, SortOrder.ASC);
            verifyNoInteractions(cinemaService);
        }

        @Test
        @DisplayName("查詢下一頁為空時應立即停止")
        void stopOnEmptyPage() throws Exception {
            stubMoviePage(1, page(List.of(new MovieRow(1, "唯一電影")), 3));
            stubMoviePage(2, page(List.of(), 3));

            byte[] result = excelService.export(ExportType.MOVIE);

            try (XSSFWorkbook workbook = readWorkbook(result)) {
                assertEquals(2, workbook.getSheet("MOVIE").getPhysicalNumberOfRows());
            }

            verify(movieService).findAll(
                    null, null, 2, PAGE_SIZE,
                    MovieSort.SortBy.MOVIE_ID, SortOrder.ASC);
            verify(movieService, never()).findAll(
                    null, null, 3, PAGE_SIZE,
                    MovieSort.SortBy.MOVIE_ID, SortOrder.ASC);
        }
    }

    @Nested
    @DisplayName("Excel 欄位內容")
    class CellValueTests {

        @Test
        @DisplayName("數字、布林值和 null 應依目前實作轉成文字儲存")
        void serializeValues() throws Exception {
            stubMoviePage(1, page(List.of(
                    new MixedRow(5, "中文名稱", false, null)), 1));

            byte[] result = excelService.export(ExportType.MOVIE);

            try (XSSFWorkbook workbook = readWorkbook(result)) {
                Sheet sheet = workbook.getSheet("MOVIE");
                assertEquals("id", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("name", sheet.getRow(0).getCell(1).getStringCellValue());
                assertEquals("active", sheet.getRow(0).getCell(2).getStringCellValue());
                assertEquals("note", sheet.getRow(0).getCell(3).getStringCellValue());

                assertEquals("5", sheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("中文名稱", sheet.getRow(1).getCell(1).getStringCellValue());
                assertEquals("false", sheet.getRow(1).getCell(2).getStringCellValue());
                assertEquals("", sheet.getRow(1).getCell(3).getStringCellValue());
            }
        }

        @Test
        @DisplayName("以 = + - @ Tab CR 開頭的內容應加上單引號防止公式注入")
        void escapeFormulaInjection() throws Exception {
            List<TextRow> rows = List.of(
                    new TextRow("=1+1"),
                    new TextRow("+SUM(A1:A2)"),
                    new TextRow("-10"),
                    new TextRow("@A1"),
                    new TextRow("\tABC"),
                    new TextRow("\rABC"),
                    new TextRow("正常文字"),
                    new TextRow(""),
                    new TextRow(null));
            stubCinemaPage(1, page(rows, rows.size()));

            byte[] result = excelService.export(ExportType.CINEMA);

            try (XSSFWorkbook workbook = readWorkbook(result)) {
                Sheet sheet = workbook.getSheet("CINEMA");
                assertEquals(10, sheet.getPhysicalNumberOfRows());
                assertEquals("value", sheet.getRow(0).getCell(0).getStringCellValue());
                assertEquals("'=1+1", sheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("'+SUM(A1:A2)", sheet.getRow(2).getCell(0).getStringCellValue());
                assertEquals("'-10", sheet.getRow(3).getCell(0).getStringCellValue());
                assertEquals("'@A1", sheet.getRow(4).getCell(0).getStringCellValue());
                assertEquals("'\tABC", sheet.getRow(5).getCell(0).getStringCellValue());
                assertEquals("'\rABC", sheet.getRow(6).getCell(0).getStringCellValue());
                assertEquals("正常文字", sheet.getRow(7).getCell(0).getStringCellValue());
                assertEquals("", sheet.getRow(8).getCell(0).getStringCellValue());
                assertEquals("", sheet.getRow(9).getCell(0).getStringCellValue());
            }
        }
    }

    @Nested
    @DisplayName("匯出時查詢失敗")
    class FailureTests {

        @Test
        @DisplayName("查詢 Service 丟出的非 IOException 應保留原始例外")
        void shouldPropagateServiceException() {
            doThrow(new IllegalStateException("查詢失敗"))
                    .when(cinemaService).findAll(
                            isNull(), isNull(), isNull(), isNull(),
                            eq(1), eq(PAGE_SIZE),
                            eq(CinemaSort.SortBy.CINEMA_ID), eq(SortOrder.ASC));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> excelService.export(ExportType.CINEMA));

            assertEquals("查詢失敗", exception.getMessage());
            verifyNoInteractions(movieService);
        }
    }
}
