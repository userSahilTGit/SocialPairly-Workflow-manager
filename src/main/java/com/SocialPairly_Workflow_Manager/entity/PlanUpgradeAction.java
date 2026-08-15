package com.SocialPairly_Workflow_Manager.entity;

public enum PlanUpgradeAction {
    Requested,
    Approved,
    Closed;

    public String getDisplayValue() {
        return name();
    }

    public static PlanUpgradeAction fromDisplayValue(String value) {
        if (value == null) return null;
        return valueOf(value);
    }
}
