
package com.collection.benefitsjourney.config;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.core.annotation.Order;

@Component
@Order(1) // 先於AdminInitializer執行
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
    
    /** 這幾張表會用到會員和特典中介表，要排在cinema_movie_benefit之後 */
    private static final java.util.Set<String> AFTER_BENEFIT = java.util.Set.of(
            "benefit_report.sql", "inventory_report.sql", "reminder.sql", "use_benefit.sql", "user_contribution.sql");

    /** 排序用的名稱：users最先，cinema_movie_benefit排在movie之後，會員互動資料排在最後 */
    private static String sortKey(String filename) {
        if ("users.sql".equals(filename)) {
            return "0" + filename;
        }
        if ("cinema_movie_benefit.sql".equals(filename)) {
            return "movie~.sql";
        }
        return AFTER_BENEFIT.contains(filename) ? "~" + filename : filename;
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
        // 照檔名排序，但cinema_movie_benefit要等cinema、movie、benefit都匯入後才能執行
        Arrays.sort(files, (a, b) ->sortKey(a.getFilename()).compareTo(sortKey(b.getFilename())));

        for (Resource file : files) {
            String name = file.getFilename();
            String table = name.substring(0, name.length() - 4);

            if (!table.matches("[a-z][a-z0-9_]*")) {
                throw new IllegalArgumentException(
                        "不合法的資料表名稱：" + table);
            }

            try (Connection connection = dataSource.getConnection()) {
                boolean h2 = connection.getMetaData().getDatabaseProductName()
                        .toLowerCase(Locale.ROOT).contains("h2");

                String sql = "SELECT TOP (1) 1 FROM dbo.[" + table + "]";

                // 檢查資料表是否已有資料
                try (Statement statement = connection.createStatement();
                        ResultSet result = statement.executeQuery(sql)) {

                    if (result.next()) {
                        log.info("略過 {}：資料表已有資料", name);
                        continue;
                    }
                }

                // 空表才執行 SQL。H2 沒有 SQL Server 的 SYSDATETIME()
                Resource script = h2 ? h2CompatibleScript(file) : file;
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);

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

    private static Resource h2CompatibleScript(Resource file) throws IOException {
        String sql = StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8)
                .replace("SYSDATETIME()", "CURRENT_TIMESTAMP");
        return new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8));
    }
}
