package com.SocialPairly_Workflow_Manager.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Encrypts binary columns (e.g. uploaded identity documents) at rest. */
@Converter
public class EncryptedBytesConverter implements AttributeConverter<byte[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(byte[] attribute) {
        return FieldEncryptor.encrypt(attribute);
    }

    @Override
    public byte[] convertToEntityAttribute(byte[] dbData) {
        return FieldEncryptor.decrypt(dbData);
    }
}
