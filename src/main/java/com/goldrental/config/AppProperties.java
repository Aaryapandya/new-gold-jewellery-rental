package com.goldrental.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed binding for application-level business configuration.
 *
 * <p>All values are loaded from {@code application.yml} under the {@code app.*} namespace.
 * Using a typed properties bean avoids scattered {@code @Value} injections across services.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Booking booking = new Booking();
    private final Search search = new Search();

    @Getter
    @Setter
    public static class Booking {
        /** Maximum number of days in advance a booking can be requested. */
        private int maxAdvanceDays = 90;

        /** Minimum rental duration in hours. */
        private int minRentalHours = 4;
    }

    @Getter
    @Setter
    public static class Search {
        /** Default search radius in kilometres when not specified by caller. */
        private double defaultRadiusKm = 50.0;

        /** Maximum allowed search radius in kilometres. */
        private double maxRadiusKm = 500.0;
    }
}
