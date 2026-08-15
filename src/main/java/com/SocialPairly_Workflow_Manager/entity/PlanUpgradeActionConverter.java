package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PlanUpgradeActionConverter implements AttributeConverter<PlanUpgradeAction, String> {

    @Override
    public String convertToDatabaseColumn(PlanUpgradeAction action) {
        return action != null ? action.getDisplayValue() : null;
    }

    @Override
    public PlanUpgradeAction convertToEntityAttribute(String dbData) {
        return PlanUpgradeAction.fromDisplayValue(dbData);
    }
}
