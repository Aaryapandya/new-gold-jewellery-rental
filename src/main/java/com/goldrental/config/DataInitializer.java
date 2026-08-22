package com.goldrental.config;

import com.goldrental.domain.entity.User;
import com.goldrental.domain.enums.UserRole;
import com.goldrental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the very first ADMIN account on application startup if none exists.
 *
 * <p>Credentials are read from {@code app.admin.*} in {@code application.yml},
 * which in turn resolves the environment variables:
 * <ul>
 *   <li>{@code ADMIN_EMAIL}    – defaults to {@code admin@goldrental.com}</li>
 *   <li>{@code ADMIN_PASSWORD} – defaults to {@code Admin@1234}</li>
 *   <li>{@code ADMIN_NAME}     – defaults to {@code Platform Admin}</li>
 * </ul>
 *
 * <p>This bean runs exactly once per startup. If an ADMIN already exists in the
 * database the seed is skipped, so it is safe to leave enabled in all environments.
 *
 * <p><strong>Production recommendation:</strong> override credentials via environment
 * variables and rotate the password after the first login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        seedBootstrapAdmin();
    }

    private void seedBootstrapAdmin() {
        final AppProperties.Admin adminProps = appProperties.getAdmin();

        // Skip if any ADMIN already exists
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.debug("Bootstrap admin seed skipped – ADMIN account already exists.");
            return;
        }

        final User admin = User.builder()
                .email(adminProps.getEmail())
                .password(passwordEncoder.encode(adminProps.getPassword()))
                .name(adminProps.getName())
                .role(UserRole.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.info("Bootstrap ADMIN account created: email={}", adminProps.getEmail());
        log.warn("⚠️  Change the default admin password immediately in production!");
    }
}
