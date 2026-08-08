package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RefundActionConverter implements AttributeConverter<RefundAction, String> {

    @Override
    public String convertToDatabaseColumn(RefundAction action) {
        return action != null ? action.getDisplayValue() : null;
    }

    @Override
    public RefundAction convertToEntityAttribute(String dbData) {
        return RefundAction.fromDisplayValue(dbData);
    }
}
