package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserIdentityCompliance;
import com.SocialPairly_Workflow_Manager.entity.UserIdentityComplianceBlob;
import com.SocialPairly_Workflow_Manager.security.FieldEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EncryptedColumnsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void sensitiveIdentityFieldsAreEncryptedAtRestAndReadBackAsPlaintext() {
        User user = new User();
        user.setFirstName("Enc");
        user.setLastName("Test");
        user.setEmail("enc@example.com");
        user.setPhoneNumber("8888888");
        user.setPassword("pw");
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(user);

        UserIdentityCompliance compliance = new UserIdentityCompliance();
        compliance.setUser(user);
        compliance.setSsn("123-45-6789");
        compliance.setIdDocumentType("PASSPORT");
        compliance.setIdDocumentNumber("X1234567");
        entityManager.persist(compliance);

        byte[] document = new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        UserIdentityComplianceBlob blob = new UserIdentityComplianceBlob();
        blob.setCompliance(compliance);
        blob.setDocPurpose("DL_FRONT");
        blob.setContentType("image/png");
        blob.setStorageBlob(document);
        entityManager.persist(blob);
        entityManager.flush();
        entityManager.clear();

        Object rawSsn = entityManager.getEntityManager()
                .createNativeQuery("SELECT ssn FROM user_identity_compliance WHERE id = ?1")
                .setParameter(1, compliance.getId())
                .getSingleResult();
        assertTrue(FieldEncryptor.isEncrypted(rawSsn.toString()));
        assertFalse(rawSsn.toString().contains("6789"));

        Object rawDocNumber = entityManager.getEntityManager()
                .createNativeQuery("SELECT id_document_number FROM user_identity_compliance WHERE id = ?1")
                .setParameter(1, compliance.getId())
                .getSingleResult();
        assertTrue(FieldEncryptor.isEncrypted(rawDocNumber.toString()));

        UserIdentityCompliance reloaded = entityManager.find(UserIdentityCompliance.class, compliance.getId());
        assertEquals("123-45-6789", reloaded.getSsn());
        assertEquals("X1234567", reloaded.getIdDocumentNumber());
        assertEquals("PASSPORT", reloaded.getIdDocumentType());

        UserIdentityComplianceBlob reloadedBlob = entityManager.find(UserIdentityComplianceBlob.class, blob.getId());
        assertArrayEquals(document, reloadedBlob.getStorageBlob());
    }
}
