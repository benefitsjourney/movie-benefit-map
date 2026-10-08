
package com.collection.benefitsjourney.config;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
public class SqlDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlDataInitializer.class);

    private final DataSource dataSource;
    private final ResourcePatternResolver resolver;

    public SqlDataInitializer(
            DataSource dataSource,
            ResourcePatternResolver resolver) {
        this.dataSource = dataSource;
        this.resolver = resolver;
    }

    /** 啟動時自動掃描並匯入空表的 SQL 資料 */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        Resource[] files = resolver.getResources(
                "classpath*:db/data/*.sql");

        if (files.length == 0) {
            log.info("沒有需要初始化的 SQL 檔案");
            return;
        }

        // 固定處理順序
        Arrays.sort(files, (a, b) -> a.getFilename().compareTo(b.getFilename()));

        for (Resource file : files) {
            String name = file.getFilename();
            String table = name.substring(0, name.length() - 4);

            if (!table.matches("[a-z][a-z0-9_]*")) {
                throw new IllegalArgumentException(
                        "不合法的資料表名稱：" + table);
            }

            try (Connection connection = dataSource.getConnection()) {

                String sql = "SELECT TOP (1) 1 FROM dbo.[" + table + "]";

                // 檢查資料表是否已有資料
                try (Statement statement = connection.createStatement();
                        ResultSet result = statement.executeQuery(sql)) {

                    if (result.next()) {
                        log.info("略過 {}：資料表已有資料", name);
                        continue;
                    }
                }

                // 空表才執行 SQL
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator(file);

                populator.setSqlScriptEncoding(
                        StandardCharsets.UTF_8.name());

                populator.setContinueOnError(false);

                boolean autoCommit = connection.getAutoCommit();

                try {
                    connection.setAutoCommit(false);
                    populator.populate(connection);
                    connection.commit();

                    log.info("匯入完成：{}", name);

                } catch (Exception error) {
                    connection.rollback();
                    log.error("匯入失敗：{}", name, error);
                    throw error;

                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            }
        }
    }
}
