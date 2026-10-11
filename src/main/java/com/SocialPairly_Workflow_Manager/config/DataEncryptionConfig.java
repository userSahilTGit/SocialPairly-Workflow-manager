package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.FieldEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads the at-rest encryption key before JPA starts, so no entity is ever read or written with a
 * different key than the one configured (defect 1584).
 */
@Configuration
public class DataEncryptionConfig {

    private static final Logger log = LoggerFactory.getLogger(DataEncryptionConfig.class);

    public DataEncryptionConfig(@Value("${app.security.data-encryption-key:}") String dataEncryptionKey) {
        if (dataEncryptionKey == null || dataEncryptionKey.isBlank()
                || FieldEncryptor.DEV_DEFAULT_KEY.equals(dataEncryptionKey.trim())) {
            log.warn("APP_DATA_ENCRYPTION_KEY is not set; using the development data-encryption key. "
                    + "Set a unique 32-byte base64 key in every shared environment.");
        }
        FieldEncryptor.configure(dataEncryptionKey);
    }

    @Bean
    static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnDataEncryption() {
        return new EntityManagerFactoryDependsOnPostProcessor("dataEncryptionConfig");
    }
}
