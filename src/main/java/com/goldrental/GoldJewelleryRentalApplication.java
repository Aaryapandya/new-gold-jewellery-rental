package com.goldrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Gold Jewellery Rental Platform.
 *
 * <p>Auditing is enabled globally so {@code @CreatedDate} / {@code @LastModifiedDate}
 * annotations on entities are populated automatically.
 */
@SpringBootApplication
@EnableJpaAuditing
public class GoldJewelleryRentalApplication {

    public static void main(final String[] args) {
        SpringApplication.run(GoldJewelleryRentalApplication.class, args);
    }
}
