package com.SocialPairly_Workflow_Manager.entity;

public enum RefundStatus {
    Initiated,
    In_Progress,
    Completed,
    Rejected;

    public String getDisplayValue() {
        return this == In_Progress ? "In-Progress" : name();
    }

    public static RefundStatus fromDisplayValue(String value) {
        if (value == null) return null;
        if ("In-Progress".equals(value)) return In_Progress;
        return valueOf(value);
    }
}
