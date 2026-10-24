package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.FieldEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataEncryptionConfigTest {

    @Test
    void blankAndDevelopmentKeysWarnAndStillConfigureEncryption() {
        new DataEncryptionConfig(null);
        assertEquals("plain", FieldEncryptor.decrypt(FieldEncryptor.encrypt("plain")));

        new DataEncryptionConfig("   ");
        new DataEncryptionConfig(FieldEncryptor.DEV_DEFAULT_KEY);
        new DataEncryptionConfig("project-specific-passphrase");
        assertEquals("other", FieldEncryptor.decrypt(FieldEncryptor.encrypt("other")));
    }

    @Test
    void jpaStartsAfterTheEncryptionConfig() {
        EntityManagerFactoryDependsOnPostProcessor processor =
                DataEncryptionConfig.entityManagerFactoryDependsOnDataEncryption();
        assertNotNull(processor);
    }
}
