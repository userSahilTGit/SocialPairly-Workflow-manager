package com.SocialPairly_Workflow_Manager.constants;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Stable dashboard keys stored in {@code role_dashboard_access.dashboard_key}. */
public final class DashboardKeys {

    public static final String DASHBOARD = "DASHBOARD";
    public static final String QUESTIONS = "QUESTIONS";
    public static final String MODERATION = "MODERATION";
    public static final String IDENTITY_DOCS = "IDENTITY_DOCS";
    public static final String PLANS = "PLANS";
    public static final String EVENTS = "EVENTS";
    public static final String FINANCE = "FINANCE";
    public static final String ROLE_ACCESS = "ROLE_ACCESS";
    public static final String CUSTOMER_SUPPORT = "CUSTOMER_SUPPORT";

    private static final Set<String> ALL = new LinkedHashSet<>(List.of(
            DASHBOARD, QUESTIONS, MODERATION, IDENTITY_DOCS, PLANS, EVENTS, FINANCE, ROLE_ACCESS, CUSTOMER_SUPPORT
    ));

    private DashboardKeys() {
    }

    public static boolean isValid(String key) {
        return key != null && ALL.contains(key.trim().toUpperCase(Locale.ROOT));
    }

    public static String normalize(String key) {
        if (key == null) {
            return null;
        }
        return key.trim().toUpperCase(Locale.ROOT);
    }

    public static Set<String> all() {
        return Set.copyOf(ALL);
    }
}
