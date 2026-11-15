package com.SocialPairly_Workflow_Manager.util;

/**
 * Helpers for profile/media URLs returned to clients.
 * Disk-era paths under {@code /uploads/} usually 404 after the BLOB migration.
 */
public final class MediaUrls {

    private MediaUrls() {
    }

    public static boolean isLegacyDiskPath(String path) {
        return path != null && path.contains("/uploads/");
    }

    /**
     * Returns the URL only when it is usable by clients; otherwise {@code null}.
     * Blank and legacy {@code /uploads/...} paths are dropped so browsers do not hit missing files.
     */
    public static String usableProfilePhotoUrl(String path) {
        if (path == null || path.isBlank() || isLegacyDiskPath(path)) {
            return null;
        }
        return path;
    }
}
