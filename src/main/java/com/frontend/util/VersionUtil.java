package com.frontend.util;

/**
 * Utility class for version comparison
 */
public class VersionUtil {

    /**
     * Compare two version strings (e.g., "1.0.0", "2.1.3")
     * @param version1 first version
     * @param version2 second version
     * @return negative if version1 < version2, 0 if equal, positive if version1 > version2
     */
    public static int compareVersions(String version1, String version2) {
        if (version1 == null || version1.trim().isEmpty()) {
            return -1;  // null/empty version is considered older
        }
        if (version2 == null || version2.trim().isEmpty()) {
            return 1;
        }

        // Remove any prefix like 'v' or 'V'
        version1 = version1.trim().toLowerCase();
        version2 = version2.trim().toLowerCase();
        if (version1.startsWith("v")) {
            version1 = version1.substring(1);
        }
        if (version2.startsWith("v")) {
            version2 = version2.substring(1);
        }

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (v1 < v2) return -1;
            if (v1 > v2) return 1;
        }

        return 0;
    }

    /**
     * Parse a version part, extracting only numeric portion
     * Handles cases like "1-beta", "2rc1", etc.
     */
    private static int parseVersionPart(String part) {
        if (part == null || part.isEmpty()) {
            return 0;
        }

        // Extract leading digits
        StringBuilder digits = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }

        if (digits.length() == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Check if clientVersion meets or exceeds minVersion
     * @param clientVersion the client's app version
     * @param minVersion the minimum required version
     * @return true if client version is >= min version
     */
    public static boolean isVersionSufficient(String clientVersion, String minVersion) {
        return compareVersions(clientVersion, minVersion) >= 0;
    }

    /**
     * Check if version string is valid (contains at least one number)
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        return version.matches(".*\\d.*");
    }
}
