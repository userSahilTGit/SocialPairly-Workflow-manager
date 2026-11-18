package com.SocialPairly_Workflow_Manager.constants;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fine-grained employee role codes stored in {@code staff_role_assignments.role_code}.
 * Coarse Spring authority remains {@code Role.ADMIN} on {@code users_details.role}.
 */
public final class EmployeeRoleCodes {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String TESTER = "TESTER";
    public static final String FINANCE = "FINANCE";
    public static final String CUSTOMER_SUPPORT = "CUSTOMER_SUPPORT";
    public static final String EVENT_TEAM = "EVENT_TEAM";
    public static final String MARKETING = "MARKETING";
    public static final String QC_LEGAL = "QC_LEGAL";

    public static final String DOMAIN_SOCIALPAIRLY = "socialpairly.com";
    public static final String DEFAULT_DOMAIN_ROLE = ADMIN;

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_DOMAIN_AUTO = "DOMAIN_AUTO";
    public static final String SOURCE_SEED = "SEED";

    private static final Set<String> ALL = new LinkedHashSet<>(List.of(
            SUPER_ADMIN, ADMIN, TESTER, FINANCE, CUSTOMER_SUPPORT, EVENT_TEAM, MARKETING, QC_LEGAL
    ));

    private EmployeeRoleCodes() {
    }

    public static boolean isValid(String roleCode) {
        return roleCode != null && ALL.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    public static String normalize(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    public static Set<String> all() {
        return Set.copyOf(ALL);
    }

    public static boolean isSocialPairlyDomain(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.lastIndexOf('@');
        if (at < 0 || at == normalized.length() - 1) {
            return false;
        }
        return DOMAIN_SOCIALPAIRLY.equals(normalized.substring(at + 1));
    }
}
