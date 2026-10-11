package com.SocialPairly_Workflow_Manager.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption for sensitive columns at rest (defect 1584).
 * <p>
 * Strings are stored as {@code enc:v1:<base64(iv || ciphertext || tag)>}; byte arrays are stored as
 * {@code SPENC1 || iv || ciphertext || tag}. Values without these markers are legacy plaintext and are
 * returned unchanged on read, so existing rows keep working until they are re-encrypted.
 * </p>
 */
public final class FieldEncryptor {

    public static final String STRING_PREFIX = "enc:v1:";
    static final byte[] BYTES_HEADER = "SPENC1".getBytes(StandardCharsets.US_ASCII);

    /** Development-only fallback; production must set APP_DATA_ENCRYPTION_KEY. */
    public static final String DEV_DEFAULT_KEY = "c3BzLWRldi1vbmx5LWRhdGEta2V5LWNoYW5nZS1tZSE=";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile SecretKeySpec key;

    private FieldEncryptor() {}

    /** Accepts a base64-encoded 32-byte key; any other value is stretched to 256 bits with SHA-256. */
    public static void configure(String rawKey) {
        key = deriveKey(rawKey);
    }

    static SecretKeySpec deriveKey(String rawKey) {
        String value = (rawKey == null || rawKey.isBlank()) ? DEV_DEFAULT_KEY : rawKey.trim();
        byte[] material = null;
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            if (decoded.length == 32) {
                material = decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // not base64 — treat as passphrase below
        }
        if (material == null) {
            try {
                material = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("SHA-256 unavailable", e);
            }
        }
        return new SecretKeySpec(material, "AES");
    }

    private static SecretKeySpec currentKey() {
        SecretKeySpec k = key;
        if (k == null) {
            synchronized (FieldEncryptor.class) {
                if (key == null) {
                    key = deriveKey(System.getenv("APP_DATA_ENCRYPTION_KEY"));
                }
                k = key;
            }
        }
        return k;
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(STRING_PREFIX);
    }

    public static boolean isEncrypted(byte[] value) {
        return value != null
                && value.length > BYTES_HEADER.length
                && Arrays.equals(value, 0, BYTES_HEADER.length, BYTES_HEADER, 0, BYTES_HEADER.length);
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || isEncrypted(plaintext)) {
            return plaintext;
        }
        byte[] sealed = seal(plaintext.getBytes(StandardCharsets.UTF_8));
        return STRING_PREFIX + Base64.getEncoder().encodeToString(sealed);
    }

    public static String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        byte[] sealed = Base64.getDecoder().decode(stored.substring(STRING_PREFIX.length()));
        return new String(open(sealed), StandardCharsets.UTF_8);
    }

    public static byte[] encrypt(byte[] plaintext) {
        if (plaintext == null || isEncrypted(plaintext)) {
            return plaintext;
        }
        byte[] sealed = seal(plaintext);
        return ByteBuffer.allocate(BYTES_HEADER.length + sealed.length).put(BYTES_HEADER).put(sealed).array();
    }

    public static byte[] decrypt(byte[] stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        return open(Arrays.copyOfRange(stored, BYTES_HEADER.length, stored.length));
    }

    private static byte[] seal(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, currentKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt sensitive field", e);
        }
    }

    private static byte[] open(byte[] sealed) {
        if (sealed.length <= IV_LENGTH) {
            throw new IllegalStateException("Encrypted field is truncated");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, currentKey(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, sealed, 0, IV_LENGTH));
            return cipher.doFinal(sealed, IV_LENGTH, sealed.length - IV_LENGTH);
        } catch (GeneralSecurityException e) {
            // Wrong key or tampered data — never fall back to returning ciphertext.
            throw new IllegalStateException("Failed to decrypt sensitive field (check APP_DATA_ENCRYPTION_KEY)", e);
        }
    }
}
