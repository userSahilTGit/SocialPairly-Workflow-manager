package com.SocialPairly_Workflow_Manager.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FieldEncryptorTest {

    private static final String TEST_KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII));

    @BeforeEach
    void setUp() {
        FieldEncryptor.configure(TEST_KEY);
    }

    @AfterEach
    void tearDown() {
        FieldEncryptor.configure(null);
    }

    @Test
    void devDefaultKeyIsA256BitBase64Key() {
        assertEquals(32, Base64.getDecoder().decode(FieldEncryptor.DEV_DEFAULT_KEY).length);
    }

    @Test
    void stringRoundTripHidesPlaintext() {
        String encrypted = FieldEncryptor.encrypt("123-45-6789");
        assertTrue(encrypted.startsWith(FieldEncryptor.STRING_PREFIX));
        assertFalse(encrypted.contains("6789"));
        assertEquals("123-45-6789", FieldEncryptor.decrypt(encrypted));
    }

    @Test
    void encryptionIsRandomizedPerValue() {
        assertNotEquals(FieldEncryptor.encrypt("same"), FieldEncryptor.encrypt("same"));
    }

    @Test
    void ssnCiphertextFitsWidenedColumn() {
        assertTrue(FieldEncryptor.encrypt("123-45-6789").length() <= 255);
    }

    @Test
    void nullsAndLegacyPlaintextPassThrough() {
        assertNull(FieldEncryptor.encrypt((String) null));
        assertNull(FieldEncryptor.decrypt((String) null));
        assertEquals("legacy-plain", FieldEncryptor.decrypt("legacy-plain"));
    }

    @Test
    void alreadyEncryptedValuesAreNotDoubleEncrypted() {
        String once = FieldEncryptor.encrypt("value");
        assertEquals(once, FieldEncryptor.encrypt(once));
    }

    @Test
    void wrongKeyFailsInsteadOfReturningCiphertext() {
        String encrypted = FieldEncryptor.encrypt("secret");
        FieldEncryptor.configure("a-different-passphrase");
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.decrypt(encrypted));
    }

    @Test
    void tamperedValueIsRejected() {
        String encrypted = FieldEncryptor.encrypt("secret");
        byte[] raw = Base64.getDecoder().decode(encrypted.substring(FieldEncryptor.STRING_PREFIX.length()));
        raw[raw.length - 1] ^= 0x01;
        String tampered = FieldEncryptor.STRING_PREFIX + Base64.getEncoder().encodeToString(raw);
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.decrypt(tampered));
    }

    @Test
    void byteRoundTripWithHeader() {
        byte[] image = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
        byte[] encrypted = FieldEncryptor.encrypt(image);
        assertTrue(FieldEncryptor.isEncrypted(encrypted));
        assertFalse(FieldEncryptor.isEncrypted(image));
        assertArrayEquals(image, FieldEncryptor.decrypt(encrypted));
        assertSame(encrypted, FieldEncryptor.encrypt(encrypted));
    }

    @Test
    void legacyBytesPassThrough() {
        byte[] legacy = new byte[] {1, 2, 3};
        assertSame(legacy, FieldEncryptor.decrypt(legacy));
        assertNull(FieldEncryptor.encrypt((byte[]) null));
    }

    @Test
    void passphraseKeysAreStretched() {
        FieldEncryptor.configure("not-base64 passphrase");
        assertEquals("hello", FieldEncryptor.decrypt(FieldEncryptor.encrypt("hello")));
    }

    @Test
    void convertersDelegateToEncryptor() {
        EncryptedStringConverter strings = new EncryptedStringConverter();
        String column = strings.convertToDatabaseColumn("acct-001");
        assertTrue(FieldEncryptor.isEncrypted(column));
        assertEquals("acct-001", strings.convertToEntityAttribute(column));

        EncryptedBytesConverter bytes = new EncryptedBytesConverter();
        byte[] stored = bytes.convertToDatabaseColumn(new byte[] {9, 8, 7});
        assertArrayEquals(new byte[] {9, 8, 7}, bytes.convertToEntityAttribute(stored));
    }

    @Test
    void shortBase64AndBlankKeysAreStretched() {
        FieldEncryptor.configure(Base64.getEncoder().encodeToString(new byte[16]));
        assertEquals("short-key", FieldEncryptor.decrypt(FieldEncryptor.encrypt("short-key")));
        FieldEncryptor.configure("   ");
        assertEquals("blank", FieldEncryptor.decrypt(FieldEncryptor.encrypt("blank")));
    }

    @Test
    void lazyKeyInitAndTruncatedPayload() throws Exception {
        Field key = FieldEncryptor.class.getDeclaredField("key");
        key.setAccessible(true);
        key.set(null, null);
        assertEquals("lazy", FieldEncryptor.decrypt(FieldEncryptor.encrypt("lazy")));

        assertFalse(FieldEncryptor.isEncrypted((byte[]) null));
        assertFalse(FieldEncryptor.isEncrypted(new byte[] {1, 2}));
        assertFalse(FieldEncryptor.isEncrypted("enc:v0:nope".getBytes(StandardCharsets.US_ASCII)));

        String truncated = FieldEncryptor.STRING_PREFIX + Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.decrypt(truncated));
    }
}
