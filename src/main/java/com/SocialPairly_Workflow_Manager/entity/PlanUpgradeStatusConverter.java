package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PlanUpgradeStatusConverter implements AttributeConverter<PlanUpgradeStatus, String> {

    @Override
    public String convertToDatabaseColumn(PlanUpgradeStatus status) {
        return status != null ? status.getDisplayValue() : null;
    }

    @Override
    public PlanUpgradeStatus convertToEntityAttribute(String dbData) {
        return PlanUpgradeStatus.fromDisplayValue(dbData);
    }
}
