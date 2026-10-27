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

    static final String READY_MESSAGE = "後端與資料庫都可連線";
    static final String UNAVAILABLE_MESSAGE = "資料庫暫時無法連線";

    /**
     * 後端與資料庫都可連線才回傳成功訊息。資料庫失敗回傳 503，讓前端繼續重試。
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute("SELECT 1");
            return ResponseEntity.ok(new HealthResponse(READY_MESSAGE));
        } catch (SQLException exception) {
            log.warn("資料庫連線檢查失敗: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new HealthResponse(UNAVAILABLE_MESSAGE));
        }
    }
}
