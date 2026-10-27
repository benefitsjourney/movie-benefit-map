package com.collection.benefitsjourney.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.HealthResponse;

@RestController
@RequestMapping({ "/health", "/api/health" })
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final int QUERY_TIMEOUT_SECONDS = 3;

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 給前端判斷用的狀態碼 */
    static final String STATUS_UP = "UP";
    static final String STATUS_DOWN = "DOWN";

    /** 後端日誌顯示 */
    private static final String READY_LOG = "後端與資料庫都可連線";
    private static final String UNAVAILABLE_LOG = "資料庫暫時無法連線";

    /**
     * 後端與資料庫都可連線才回傳 UP。資料庫失敗回傳 503 與 DOWN，讓前端繼續重試。
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute("SELECT 1");
            log.info(READY_LOG);
            return ResponseEntity.ok(new HealthResponse(STATUS_UP));
        } catch (SQLException exception) {
            log.warn("{}: {}", UNAVAILABLE_LOG, exception.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new HealthResponse(STATUS_DOWN));
        }
    }
}
