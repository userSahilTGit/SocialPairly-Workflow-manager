package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.FieldEncryptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

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
}
