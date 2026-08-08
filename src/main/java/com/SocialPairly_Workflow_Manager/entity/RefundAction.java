package com.SocialPairly_Workflow_Manager.entity;

public enum RefundAction {
    Requested,
    Requested_a_Call,
    Provided_Slot,
    Approved,
    Provided_Bank_Details,
    Closed;

    public String getDisplayValue() {
        return switch (this) {
            case Requested_a_Call -> "Requested a Call";
            case Provided_Slot -> "Provided Slot";
            case Provided_Bank_Details -> "Provided Bank Details";
            default -> name();
        };
    }

    public static RefundAction fromDisplayValue(String value) {
        if (value == null) return null;
        return switch (value) {
            case "Requested a Call" -> Requested_a_Call;
            case "Provided Slot" -> Provided_Slot;
            case "Provided Bank Details" -> Provided_Bank_Details;
            default -> valueOf(value);
        };
    }
}
