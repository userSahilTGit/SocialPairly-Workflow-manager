package com.SocialPairly_Workflow_Manager.entity;

public enum PlanUpgradeStatus {
    Started,
    InProgress,
    Completed,
    Rejected;

    public String getDisplayValue() {
        return name();
    }

    public static PlanUpgradeStatus fromDisplayValue(String value) {
        if (value == null) return null;
        return valueOf(value);
    }
}
