package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RefundStatusConverter implements AttributeConverter<RefundStatus, String> {

    @Override
    public String convertToDatabaseColumn(RefundStatus status) {
        return status != null ? status.getDisplayValue() : null;
    }

    @Override
    public RefundStatus convertToEntityAttribute(String dbData) {
        return RefundStatus.fromDisplayValue(dbData);
    }
}
