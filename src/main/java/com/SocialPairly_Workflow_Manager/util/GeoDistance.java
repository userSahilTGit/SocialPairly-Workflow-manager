package com.SocialPairly_Workflow_Manager.util;

public final class GeoDistance {

    private static final double EARTH_RADIUS_MILES = 3958.7613;

    private GeoDistance() {
    }

    public static double milesBetween(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_MILES * c;
    }

    public static boolean withinMiles(Double lat1, Double lon1, Double lat2, Double lon2, double maxMiles) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return false;
        }
        return milesBetween(lat1, lon1, lat2, lon2) <= maxMiles;
    }
}
