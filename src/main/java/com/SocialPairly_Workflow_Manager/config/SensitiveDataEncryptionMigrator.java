package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.FieldEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * One-time, idempotent upgrade for encrypted columns (defect 1584):
 * <ol>
 *   <li>widens VARCHAR columns that are too short for ciphertext ({@code ddl-auto=update} never widens columns);</li>
 *   <li>encrypts rows that still hold legacy plaintext.</li>
 * </ol>
 * Failures are logged and never stop the application from starting.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SensitiveDataEncryptionMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataEncryptionMigrator.class);

    record TextColumn(String table, String idColumn, String column, int minLength) {}

    static final List<TextColumn> TEXT_COLUMNS = List.of(
            new TextColumn("user_identity_compliance", "id", "ssn", 255),
            new TextColumn("user_identity_compliance", "id", "id_document_number", 255),
            new TextColumn("bank_details", "id", "account_number", 255),
            new TextColumn("bank_details", "id", "aba_routing_number", 255),
            new TextColumn("bank_details", "id", "account_holder_name", 512),
            new TextColumn("bank_details", "id", "recipients_address", 0)
    );

    private static final String BLOB_TABLE = "user_identity_compliance_blobs";
    private static final String BLOB_COLUMN = "storage_blob";

    private final DataSource dataSource;
    private final boolean enabled;

    public SensitiveDataEncryptionMigrator(
            DataSource dataSource,
            @Value("${app.security.data-encryption.migrate-on-startup:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        boolean mysql = isMySql();
        for (TextColumn column : TEXT_COLUMNS) {
            try {
                if (mysql && !ensureVarcharCapacity(jdbc, column)) {
                    continue;
                }
                int encrypted = encryptPlaintextRows(jdbc, column);
                if (encrypted > 0) {
                    log.info("Encrypted {} existing value(s) in {}.{}", encrypted, column.table(), column.column());
                }
            } catch (BadSqlGrammarException e) {
                log.warn("Skipped encrypting {}.{} (table or column not present)", column.table(), column.column());
            } catch (Exception e) {
                log.error("Could not migrate {}.{} to encrypted storage: {}",
                        column.table(), column.column(), e.getMessage());
            }
        }
        try {
            int encrypted = encryptPlaintextBlobs(jdbc, mysql);
            if (encrypted > 0) {
                log.info("Encrypted {} existing identity document(s) in {}", encrypted, BLOB_TABLE);
            }
        } catch (BadSqlGrammarException e) {
            log.warn("Skipped encrypting {}.{} (table or column not present)", BLOB_TABLE, BLOB_COLUMN);
        } catch (Exception e) {
            log.error("Could not migrate {}.{} to encrypted storage: {}", BLOB_TABLE, BLOB_COLUMN, e.getMessage());
        }
    }

    private boolean isMySql() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns false when the column is missing or could not be widened (rows are then left untouched). */
    boolean ensureVarcharCapacity(JdbcTemplate jdbc, TextColumn column) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                column.table(), column.column());
        if (rows.isEmpty()) {
            return false;
        }
        Map<String, Object> info = rows.get(0);
        String dataType = String.valueOf(info.get("DATA_TYPE")).toLowerCase();
        if (!"varchar".equals(dataType) || column.minLength() <= 0) {
            return true;
        }
        Number current = (Number) info.get("CHARACTER_MAXIMUM_LENGTH");
        if (current != null && current.longValue() >= column.minLength()) {
            return true;
        }
        String nullability = "NO".equalsIgnoreCase(String.valueOf(info.get("IS_NULLABLE"))) ? "NOT NULL" : "NULL";
        jdbc.execute("ALTER TABLE " + column.table() + " MODIFY COLUMN " + column.column()
                + " VARCHAR(" + column.minLength() + ") " + nullability);
        log.info("Widened {}.{} to VARCHAR({}) for encrypted storage", column.table(), column.column(), column.minLength());
        return true;
    }

    int encryptPlaintextRows(JdbcTemplate jdbc, TextColumn column) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT " + column.idColumn() + " AS row_id, " + column.column() + " AS val FROM " + column.table()
                        + " WHERE " + column.column() + " IS NOT NULL AND " + column.column() + " NOT LIKE ?",
                FieldEncryptor.STRING_PREFIX + "%");
        int updated = 0;
        for (Map<String, Object> row : rows) {
            Object value = row.get("val");
            if (value == null) {
                continue;
            }
            String plaintext = value.toString();
            if (FieldEncryptor.isEncrypted(plaintext)) {
                continue;
            }
            updated += jdbc.update(
                    "UPDATE " + column.table() + " SET " + column.column() + " = ? WHERE " + column.idColumn()
                            + " = ? AND " + column.column() + " = ?",
                    FieldEncryptor.encrypt(plaintext), row.get("row_id"), plaintext);
        }
        return updated;
    }

    int encryptPlaintextBlobs(JdbcTemplate jdbc, boolean mysql) {
        String sql = "SELECT id FROM " + BLOB_TABLE + " WHERE " + BLOB_COLUMN + " IS NOT NULL";
        List<Long> ids = mysql
                ? jdbc.queryForList(sql + " AND LEFT(" + BLOB_COLUMN + ", 6) <> 'SPENC1'", Long.class)
                : jdbc.queryForList(sql, Long.class);
        int updated = 0;
        for (Long id : ids) {
            byte[] data = jdbc.query(
                    "SELECT " + BLOB_COLUMN + " FROM " + BLOB_TABLE + " WHERE id = ?",
                    rs -> rs.next() ? rs.getBytes(1) : null,
                    id);
            if (data == null || data.length == 0 || FieldEncryptor.isEncrypted(data)) {
                continue;
            }
            updated += jdbc.update(
                    "UPDATE " + BLOB_TABLE + " SET " + BLOB_COLUMN + " = ? WHERE id = ?",
                    FieldEncryptor.encrypt(data), id);
        }
        return updated;
    }
}
