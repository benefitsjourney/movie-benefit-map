
package com.collection.benefitsjourney.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * 啟動時載入 db/data/*.sql 的預設資料。
 *
 * <p>
 * 同時支援 SQL Server 與 H2（MODE=MSSQLServer）：
 * 先修正兩種資料庫對 UNIQUE / NULL 的處理差異，再依外鍵相依順序匯入空表。
 * 已有資料的表不會覆蓋，單一檔案失敗時會回滾該檔案的匯入。
 */
@Component
@Order(1) // 必須先於 AdminInitializer 執行
public class SqlDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlDataInitializer.class);
    private static final String DATA_SCRIPTS = "classpath*:db/data/*.sql";

    /** 這些資料會參照會員、電影或特典，因此安排在主要資料匯入後。 */
    private static final Set<String> AFTER_BENEFIT = Set.of(
            "benefit_report.sql", "inventory_report.sql", "reminder.sql", "use_benefit.sql", "user_contribution.sql");

    private final DataSource dataSource;
    private final ResourcePatternResolver resolver;

    public SqlDataInitializer(DataSource dataSource, ResourcePatternResolver resolver) {
        this.dataSource = dataSource;
        this.resolver = resolver;
    }

    /** users 最先；電影與特典的中介表在 movie 之後；會員互動資料最後。 */
    private static String sortKey(String filename) {
        if ("users.sql".equals(filename)) {
            return "0" + filename;
        }
        if ("cinema_movie_benefit.sql".equals(filename)) {
            return "movie~.sql";
        }
        return AFTER_BENEFIT.contains(filename) ? "~" + filename : filename;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean h2;

        // Hibernate 建表後、匯入種子資料前，先修正 UNIQUE 對 NULL 的判定。
        try (Connection connection = dataSource.getConnection()) {
            h2 = isH2(connection);
            prepareNullableUniqueKeys(connection);
        }

        Resource[] files = resolver.getResources(DATA_SCRIPTS);
        if (files.length == 0) {
            log.info("沒有需要初始化的 SQL 檔案");
            return;
        }

        Arrays.sort(files, (a, b) -> sortKey(a.getFilename()).compareTo(sortKey(b.getFilename())));

        for (Resource file : files) {
            importIfEmpty(file, h2);
        }
    }

    /** 只匯入空表；每個檔案有獨立交易，避免部分寫入。 */
    private void importIfEmpty(Resource file, boolean h2) throws Exception {
        String filename = file.getFilename();
        if (filename == null || !filename.endsWith(".sql")) {
            throw new IllegalArgumentException("不合法的 SQL 檔案名稱：" + filename);
        }
        String table = filename.substring(0, filename.length() - 4);
        requireDataTableName(table);

        try (Connection connection = dataSource.getConnection()) {
            if (tableHasRows(connection, table)) {
                log.info("略過 {}：資料表已有資料", filename);
                return;
            }

            // H2 不支援 SQL Server 的 SYSDATETIME()，只在 H2 時轉換。
            Resource script = h2 ? h2CompatibleScript(file) : file;
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);
            populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
            populator.setContinueOnError(false);

            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                populator.populate(connection);
                connection.commit();
                log.info("匯入完成：{}", filename);
            } catch (Exception error) {
                connection.rollback();
                log.error("匯入失敗：{}", filename, error);
                throw error;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    /**
     * TOP (1) 同時適用 SQL Server 與 H2 的 MSSQLServer 模式。
     * 表名只來自通過驗證的 SQL 檔名，避免組合 SQL 時被注入。
     */
    private boolean tableHasRows(Connection connection, String table) throws Exception {
        String sql = "SELECT TOP (1) 1 FROM dbo.[" + table + "]";
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            return result.next();
        }
    }

    /** 限定要修正的欄位，避免影響其他表的唯一性規則。 */
    private void prepareNullableUniqueKeys(Connection connection) throws Exception {
        // 一般登入無 google_id；Google 登入無 account；手動電影無 source_id。
        allowMultipleNulls(connection, "users", "account");
        allowMultipleNulls(connection, "users", "google_id");
        allowMultipleNulls(connection, "movie", "source_id");

        // MOVIE 提醒的 benefit_id 為 NULL；BENEFIT 提醒的 movie_id 為 NULL。
        // 仍需禁止同一會員重複追蹤「相同」電影或「相同」特典。
        allowDistinctNulls(connection, "reminder", "uk_reminder_user_type_benefit",
                "user_id, reminder_type, benefit_id", "benefit_id");
        allowDistinctNulls(connection, "reminder", "uk_reminder_user_type_movie",
                "user_id, reminder_type, movie_id", "movie_id");
    }

    private static boolean isH2(Connection connection) throws Exception {
        return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("h2");
    }

    /** H2 用 NULLS DISTINCT；SQL Server 用 WHERE column IS NOT NULL 的篩選索引。 */
    private void allowMultipleNulls(Connection connection, String table, String column) throws Exception {
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (product.contains("h2")) {
            allowMultipleNullsOnH2(connection, table, column);
        } else if (product.contains("microsoft")) {
            allowMultipleNullsOnSqlServer(connection, table, column);
        }
    }

    private void allowMultipleNullsOnH2(Connection connection, String table, String column) throws Exception {
        String constraintName = null;
        String sql = """
                SELECT tc.CONSTRAINT_NAME, tc.NULLS_DISTINCT
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                  ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                 AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                 AND tc.TABLE_NAME = kcu.TABLE_NAME
                WHERE tc.TABLE_SCHEMA = 'DBO'
                  AND tc.TABLE_NAME = ?
                  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
                  AND kcu.COLUMN_NAME = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu2
                      WHERE kcu2.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
                        AND kcu2.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                        AND kcu2.TABLE_NAME = tc.TABLE_NAME
                        AND kcu2.COLUMN_NAME <> ?
                  )
                """;
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, table.toUpperCase(Locale.ROOT));
            query.setString(2, column.toUpperCase(Locale.ROOT));
            query.setString(3, column.toUpperCase(Locale.ROOT));
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    // 已處理過的約束不用每次啟動都刪除、重建。
                    if ("YES".equalsIgnoreCase(result.getString("NULLS_DISTINCT"))) {
                        return;
                    }
                    constraintName = result.getString(1);
                }
            }
        }
        if (constraintName == null) {
            return;
        }
        requireIdent(constraintName);
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE dbo." + table + " DROP CONSTRAINT " + constraintName);
            statement.execute("ALTER TABLE dbo." + table + " ADD CONSTRAINT " + constraintName
                    + " UNIQUE NULLS DISTINCT (" + column + ")");
        }
    }

    private void allowMultipleNullsOnSqlServer(Connection connection, String table, String column) throws Exception {
        String indexName = "UX_" + table + "_" + column;
        String find = """
                SELECT kc.name
                FROM sys.key_constraints kc
                INNER JOIN sys.tables t ON t.object_id = kc.parent_object_id
                INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
                INNER JOIN sys.index_columns ic
                    ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id AND ic.key_ordinal = 1
                INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                WHERE s.name = 'dbo' AND t.name = ? AND kc.type = 'UQ' AND c.name = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM sys.index_columns ic2
                      WHERE ic2.object_id = ic.object_id AND ic2.index_id = ic.index_id
                        AND ic2.key_ordinal > 0 AND ic2.column_id <> c.column_id
                  )
                """;
        try (PreparedStatement query = connection.prepareStatement(find)) {
            query.setString(1, table);
            query.setString(2, column);
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    String constraintName = requireIdent(result.getString(1));
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE dbo." + table + " DROP CONSTRAINT [" + constraintName + "]");
                    }
                }
            }
        }

        String exists = """
                SELECT 1
                FROM sys.indexes i
                INNER JOIN sys.tables t ON t.object_id = i.object_id
                INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
                WHERE s.name = 'dbo' AND t.name = ? AND i.name = ?
                """;
        try (PreparedStatement query = connection.prepareStatement(exists)) {
            query.setString(1, table);
            query.setString(2, indexName);
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX " + indexName
                    + " ON dbo." + table + "(" + column + ") WHERE " + column + " IS NOT NULL");
        }
    }

    /**
     * 複合唯一索引裡的可為 NULL 欄位要允許多筆 NULL，有值時仍然不可重複。
     * H2 的 MSSQL 模式用 NULLS DISTINCT 約束；SQL Server 用篩選索引。
     */
    private void allowDistinctNulls(Connection connection, String table, String keyName,
            String columns, String nullableColumn) throws Exception {
        requireIdent(table);
        requireIdent(keyName);
        requireIdent(nullableColumn);
        for (String part : columns.split(",")) {
            requireIdent(part.trim());
        }
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (product.contains("h2")) {
            allowDistinctNullsOnH2(connection, table, keyName, columns);
        } else if (product.contains("microsoft")) {
            allowDistinctNullsOnSqlServer(connection, table, keyName, columns, nullableColumn);
        }
    }

    private void allowDistinctNullsOnH2(Connection connection, String table, String constraintName, String columns)
            throws Exception {
        // 注意：Hibernate 可能建立的是 UNIQUE CONSTRAINT，而非同名 INDEX。
        // 只額外 CREATE INDEX，原本的約束仍會把多筆 NULL 當成重複並拒絕 INSERT。
        String sql = """
                SELECT NULLS_DISTINCT
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = 'DBO'
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                  AND CONSTRAINT_TYPE = 'UNIQUE'
                """;
        boolean constraintExists = false;
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, table.toUpperCase(Locale.ROOT));
            query.setString(2, constraintName.toUpperCase(Locale.ROOT));
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    constraintExists = true;
                    if ("YES".equalsIgnoreCase(result.getString(1))) {
                        return;
                    }
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            if (constraintExists) {
                statement.execute("ALTER TABLE dbo." + table + " DROP CONSTRAINT " + constraintName);
            }

            // 清除舊初始化器可能建立的同名 INDEX，再以約束形式保留原名稱。
            statement.execute("DROP INDEX IF EXISTS dbo." + constraintName);
            statement.execute("ALTER TABLE dbo." + table + " ADD CONSTRAINT " + constraintName
                    + " UNIQUE NULLS DISTINCT (" + columns + ")");
        }
    }

    private void allowDistinctNullsOnSqlServer(Connection connection, String table, String indexName,
            String columns, String nullableColumn) throws Exception {
        // 若原本是 Hibernate 產生的 UNIQUE CONSTRAINT，要先移除約束本身；
        // SQL Server 不能用 DROP INDEX 直接刪除約束所擁有的索引。
        String findConstraint = """
                SELECT 1
                FROM sys.key_constraints kc
                INNER JOIN sys.tables t ON t.object_id = kc.parent_object_id
                INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
                WHERE s.name = 'dbo' AND t.name = ? AND kc.name = ? AND kc.type = 'UQ'
                """;
        try (PreparedStatement query = connection.prepareStatement(findConstraint)) {
            query.setString(1, table);
            query.setString(2, indexName);
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE dbo." + table + " DROP CONSTRAINT [" + indexName + "]");
                    }
                }
            }
        }

        String find = """
                SELECT i.has_filter, i.filter_definition
                FROM sys.indexes i
                INNER JOIN sys.tables t ON t.object_id = i.object_id
                INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
                WHERE s.name = 'dbo' AND t.name = ? AND i.name = ?
                """;
        boolean found = false;
        try (PreparedStatement query = connection.prepareStatement(find)) {
            query.setString(1, table);
            query.setString(2, indexName);
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    found = true;
                    String definition = result.getString(2);
                    if (result.getBoolean(1) && definition != null
                            && definition.toLowerCase(Locale.ROOT).contains(nullableColumn.toLowerCase(Locale.ROOT))) {
                        return;
                    }
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            if (found) {
                statement.execute("DROP INDEX " + indexName + " ON dbo." + table);
            }
            statement.execute("CREATE UNIQUE INDEX " + indexName
                    + " ON dbo." + table + " (" + columns + ") WHERE " + nullableColumn + " IS NOT NULL");
        }
    }

    private static String requireIdent(String name) {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalStateException("不合法的識別名稱：" + name);
        }
        return name;
    }

    /** db/data/ 的檔名會用來組成資料表名稱，僅允許小寫英文、數字和底線。 */
    private static void requireDataTableName(String table) {
        if (!table.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("不合法的資料表名稱：" + table);
        }
    }

    private static Resource h2CompatibleScript(Resource file) throws IOException {
        String sql = StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8)
                .replace("SYSDATETIME()", "CURRENT_TIMESTAMP");
        return new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8));
    }
}
