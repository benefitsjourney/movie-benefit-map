
package com.collection.benefitsjourney.service.CinemaMovie;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaMovieReviewSort;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaResourceType;
import com.collection.benefitsjourney.enums.CinemaMovie.CinemaSort;
import com.collection.benefitsjourney.enums.CinemaMovie.MovieSort;
import com.collection.benefitsjourney.enums.CrawlerLogSort;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.service.CrawlerLogService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class CinemaMovieExcelService {

    private static final int SIZE = 200;

    private final CinemaService cinemaService;
    private final MovieService movieService;
    private final CrawlerLogService crawlerLogService;
    private final CinemaMovieReviewsService cinemaMovieReviewsService;
    private final JsonMapper mapper;

    /** 匯出指定類型的全部資料 */
    public byte[] export(CinemaResourceType type) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(type.name());

            int page = 1;
            int row = 0;
            int count = 0;

            while (true) {

                PageResponse<?> response = findAll(type, page++, SIZE);

                if (response.content().isEmpty()) {
                    break;
                }

                for (Object item : response.content()) {

                    JsonNode json = mapper.valueToTree(item);

                    if (row == 0) {
                        writeRow(sheet, row++, json, true);
                    }

                    writeRow(sheet, row++, json, false);
                    count++;
                }

                if (count >= response.totalElements()) {
                    break;
                }
            }

            workbook.write(output);
            return output.toByteArray();

        } catch (IOException e) {
            throw badRequest("Excel 匯出失敗：" + e.getMessage());
        }
    }

    /**
     * 將 JSON 欄位寫入 Excel。
     * entityName 只給畫面顯示，資料表沒有這一欄，寫進 Excel 會讓種子 SQL 對不上。
     */
    private void writeRow(Sheet sheet, int index, JsonNode json, boolean header) {

        Row row = sheet.createRow(index);
        int column = 0;

        for (Map.Entry<String, JsonNode> field : json.properties()) {
            if ("entityName".equals(field.getKey())) {
                continue;
            }

            JsonNode value = field.getValue();

            String text = header
                    ? field.getKey()
                    : value == null || value.isNull() ? ""
                            : value.isString() ? value.stringValue() : value.toString();

            row.createCell(column++).setCellValue(escapeFormula(text, header));
        }
    }

    /** 避免 Excel 公式注入 */
    private String escapeFormula(String text, boolean header) {
        if (!header && text != null && !text.isEmpty()
                && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text == null ? "" : text;
    }

    /** 根據類型取得指定頁面的資料 */
    private PageResponse<?> findAll(CinemaResourceType type, int page, int size) {

        return switch (type) {

            case CINEMA -> cinemaService.findAll(
                    null, null, null, null,
                    page, size,
                    CinemaSort.SortBy.CINEMA_ID,
                    SortOrder.ASC);

            case MOVIE -> movieService.findAll(
                    null, null,
                    page, size,
                    MovieSort.SortBy.MOVIE_ID,
                    SortOrder.ASC);

            case CRAWLER_LOG -> crawlerLogService.findAll(
                    null, null,
                    page, size,
                    CrawlerLogSort.SortBy.CRAWLER_LOG_ID,
                    SortOrder.ASC);

            case CINEMA_MOVIE_REVIEW -> cinemaMovieReviewsService.findAll(
                    null, null,
                    page, size,
                    CinemaMovieReviewSort.SortBy.CINEMA_MOVIE_REVIEW_ID,
                    SortOrder.ASC);
        };
    }
}
