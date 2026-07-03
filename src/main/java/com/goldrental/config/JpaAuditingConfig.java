package com.goldrental.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA Auditing so that {@code @CreatedDate} and {@code @LastModifiedDate}
 * fields on entities are automatically populated by Spring Data.
 *
 * <p>Auditing is already enabled via {@code @EnableJpaAuditing} on the main
 * application class; this configuration class provides a single place to extend
 * auditing (e.g., adding an {@code AuditorAware} bean for created-by / modified-by
 * tracking) in the future without touching the entry point.
 */
@Configuration
public class JpaAuditingConfig {
    // AuditorAware bean can be added here in a future iteration
    // to capture createdBy / modifiedBy fields if required.
}
