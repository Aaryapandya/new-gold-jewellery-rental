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
    private final Search  search  = new Search();
    private final Admin   admin   = new Admin();

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

    @Getter
    @Setter
    public static class Admin {
        /** Bootstrap admin email – seeded once at startup if no admin exists. */
        private String email = "admin@goldrental.com";

        /** Bootstrap admin password (BCrypt-encoded before storage). */
        private String password = "Admin@1234";

        /** Bootstrap admin display name. */
        private String name = "Platform Admin";
    }
}
