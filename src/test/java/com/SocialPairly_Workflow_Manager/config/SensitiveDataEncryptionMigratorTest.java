package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.FieldEncryptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveDataEncryptionMigratorTest {

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        FieldEncryptor.configure(null);
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:enc_migrator_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE user_identity_compliance (id BIGINT PRIMARY KEY, ssn VARCHAR(255), "
                + "id_document_number VARCHAR(255))");
        jdbc.execute("CREATE TABLE bank_details (id BIGINT PRIMARY KEY, account_number VARCHAR(255), "
                + "aba_routing_number VARCHAR(255), account_holder_name VARCHAR(512), recipients_address TEXT)");
        jdbc.execute("CREATE TABLE user_identity_compliance_blobs (id BIGINT PRIMARY KEY, storage_blob BLOB)");
    }

    @AfterEach
    void tearDown() {
        jdbc.execute("SHUTDOWN");
    }

    @Test
    void encryptsLegacyPlaintextAndIsIdempotent() {
        String alreadyEncrypted = FieldEncryptor.encrypt("999-99-9999");
        jdbc.update("INSERT INTO user_identity_compliance VALUES (1, '123-45-6789', 'D1234567')");
        jdbc.update("INSERT INTO user_identity_compliance VALUES (2, ?, NULL)", alreadyEncrypted);
        jdbc.update("INSERT INTO bank_details VALUES (1, '000123456789', '021000021', 'Jane Doe', '1 Main St')");
        jdbc.update("INSERT INTO user_identity_compliance_blobs VALUES (1, ?)", (Object) new byte[] {1, 2, 3, 4, 5, 6, 7});

        SensitiveDataEncryptionMigrator migrator = new SensitiveDataEncryptionMigrator(dataSource, true);
        migrator.run(null);

        String ssn = jdbc.queryForObject("SELECT ssn FROM user_identity_compliance WHERE id = 1", String.class);
        assertTrue(FieldEncryptor.isEncrypted(ssn));
        assertEquals("123-45-6789", FieldEncryptor.decrypt(ssn));
        assertEquals("D1234567", FieldEncryptor.decrypt(jdbc.queryForObject(
                "SELECT id_document_number FROM user_identity_compliance WHERE id = 1", String.class)));
        assertEquals(alreadyEncrypted,
                jdbc.queryForObject("SELECT ssn FROM user_identity_compliance WHERE id = 2", String.class));
        assertNull(jdbc.queryForObject("SELECT id_document_number FROM user_identity_compliance WHERE id = 2", String.class));

        String account = jdbc.queryForObject("SELECT account_number FROM bank_details WHERE id = 1", String.class);
        assertEquals("000123456789", FieldEncryptor.decrypt(account));
        assertEquals("1 Main St", FieldEncryptor.decrypt(jdbc.queryForObject(
                "SELECT recipients_address FROM bank_details WHERE id = 1", String.class)));

        byte[] blob = jdbc.queryForObject("SELECT storage_blob FROM user_identity_compliance_blobs WHERE id = 1", byte[].class);
        assertTrue(FieldEncryptor.isEncrypted(blob));
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7}, FieldEncryptor.decrypt(blob));

        migrator.run(null);
        assertEquals(ssn, jdbc.queryForObject("SELECT ssn FROM user_identity_compliance WHERE id = 1", String.class));
        assertArrayEquals(blob, jdbc.queryForObject(
                "SELECT storage_blob FROM user_identity_compliance_blobs WHERE id = 1", byte[].class));
    }

    @Test
    void disabledMigratorLeavesDataUntouched() {
        jdbc.update("INSERT INTO user_identity_compliance VALUES (1, '123-45-6789', NULL)");
        new SensitiveDataEncryptionMigrator(dataSource, false).run(null);
        assertEquals("123-45-6789", jdbc.queryForObject("SELECT ssn FROM user_identity_compliance WHERE id = 1", String.class));
    }

    @Test
    void missingTablesDoNotFailStartup() {
        jdbc.execute("DROP TABLE bank_details");
        jdbc.execute("DROP TABLE user_identity_compliance_blobs");
        assertDoesNotThrow(() -> new SensitiveDataEncryptionMigrator(dataSource, true).run(null));
    }

    @Test
    void connectionFailureIsLoggedAndDoesNotStopStartup() {
        DataSource broken = new DriverManagerDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("down");
            }
        };
        assertDoesNotThrow(() -> new SensitiveDataEncryptionMigrator(broken, true).run(null));
    }

    @Test
    void mysqlProductNameRunsColumnWidenAndEncryptsRows() {
        DataSource mysqlLooking = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class[]{DataSource.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        Connection connection = dataSource.getConnection();
                        connection.setAutoCommit(true);
                        return mysqlConnection(connection);
                    }
                    if ("unwrap".equals(method.getName())) {
                        return dataSource;
                    }
                    if ("isWrapperFor".equals(method.getName())) {
                        return false;
                    }
                    return null;
                });
        jdbc.update("INSERT INTO user_identity_compliance VALUES (9, '111-22-3333', 'DOC')");
        jdbc.execute("ALTER TABLE bank_details DROP COLUMN aba_routing_number");

        assertDoesNotThrow(() -> new SensitiveDataEncryptionMigrator(mysqlLooking, true).run(null));
    }

    @Test
    void columnCapacityAndPlaintextHelpersCoverSkipPaths() {
        SensitiveDataEncryptionMigrator migrator = new SensitiveDataEncryptionMigrator(dataSource, true);
        JdbcTemplate template = mock(JdbcTemplate.class);
        SensitiveDataEncryptionMigrator.TextColumn ssn =
                SensitiveDataEncryptionMigrator.TEXT_COLUMNS.get(0);
        SensitiveDataEncryptionMigrator.TextColumn address =
                SensitiveDataEncryptionMigrator.TEXT_COLUMNS.get(5);

        when(template.queryForList(contains("INFORMATION_SCHEMA"), eq(ssn.table()), eq(ssn.column())))
                .thenReturn(List.of());
        assertFalse(migrator.ensureVarcharCapacity(template, ssn));

        when(template.queryForList(contains("INFORMATION_SCHEMA"), eq(address.table()), eq(address.column())))
                .thenReturn(List.of(Map.of("DATA_TYPE", "text", "IS_NULLABLE", "YES")));
        assertTrue(migrator.ensureVarcharCapacity(template, address));

        when(template.queryForList(contains("INFORMATION_SCHEMA"), eq(ssn.table()), eq(ssn.column())))
                .thenReturn(List.of(Map.of(
                        "DATA_TYPE", "varchar",
                        "CHARACTER_MAXIMUM_LENGTH", 255,
                        "IS_NULLABLE", "YES")));
        assertTrue(migrator.ensureVarcharCapacity(template, ssn));

        when(template.queryForList(contains("INFORMATION_SCHEMA"), eq(ssn.table()), eq(ssn.column())))
                .thenReturn(List.of(Map.of(
                        "DATA_TYPE", "varchar",
                        "CHARACTER_MAXIMUM_LENGTH", 20,
                        "IS_NULLABLE", "NO")));
        assertTrue(migrator.ensureVarcharCapacity(template, ssn));

        Map<String, Object> missingLength = new HashMap<>();
        missingLength.put("DATA_TYPE", "varchar");
        missingLength.put("CHARACTER_MAXIMUM_LENGTH", null);
        missingLength.put("IS_NULLABLE", "YES");
        when(template.queryForList(contains("INFORMATION_SCHEMA"), eq(ssn.table()), eq(ssn.column())))
                .thenReturn(List.of(missingLength));
        assertTrue(migrator.ensureVarcharCapacity(template, ssn));

        String already = FieldEncryptor.encrypt("kept");
        when(template.queryForList(contains("AS val"), eq(FieldEncryptor.STRING_PREFIX + "%")))
                .thenReturn(List.of(
                        Map.of("row_id", 1, "val", already),
                        Map.of("row_id", 2),
                        Map.of("row_id", 3, "val", "plain-ssn")));
        when(template.update(anyString(), any(), any(), any())).thenReturn(1);
        assertEquals(1, migrator.encryptPlaintextRows(template, ssn));

        when(template.queryForList(contains("SPENC1"), eq(Long.class))).thenReturn(List.of(1L, 2L, 3L));
        when(template.query(anyString(), any(ResultSetExtractor.class), any())).thenAnswer(invocation -> {
            Object id = invocation.getArgument(2);
            if (Long.valueOf(1L).equals(id)) {
                return null;
            }
            if (Long.valueOf(2L).equals(id)) {
                return new byte[0];
            }
            return FieldEncryptor.encrypt(new byte[]{1, 2, 3, 4, 5, 6});
        });
        assertEquals(0, migrator.encryptPlaintextBlobs(template, true));

        when(template.queryForList(contains("storage_blob IS NOT NULL"), eq(Long.class))).thenReturn(List.of(4L));
        when(template.query(anyString(), any(ResultSetExtractor.class), eq(4L))).thenReturn(new byte[]{7, 7, 7});
        when(template.update(anyString(), any(byte[].class), eq(4L))).thenReturn(1);
        assertEquals(1, migrator.encryptPlaintextBlobs(template, false));
    }

    private static Connection mysqlConnection(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        try {
                            if (!real.getAutoCommit()) {
                                real.commit();
                            }
                        } catch (SQLException ignored) {
                            // auto-commit connections reject an extra commit
                        }
                    }
                    if ("getMetaData".equals(method.getName())) {
                        DatabaseMetaData meta = real.getMetaData();
                        return Proxy.newProxyInstance(
                                DatabaseMetaData.class.getClassLoader(),
                                new Class[]{DatabaseMetaData.class},
                                (metaProxy, metaMethod, metaArgs) -> {
                                    if ("getDatabaseProductName".equals(metaMethod.getName())) {
                                        return "MySQL";
                                    }
                                    try {
                                        return metaMethod.invoke(meta, metaArgs);
                                    } catch (InvocationTargetException ex) {
                                        throw ex.getCause();
                                    }
                                });
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                    }
                });
    }
}
