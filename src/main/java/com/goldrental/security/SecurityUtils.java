package com.goldrental.security;

import com.goldrental.domain.entity.User;
import com.goldrental.exception.ResourceNotFoundException;
import com.goldrental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility for extracting the currently authenticated user from the security context.
 *
 * <p>Services call {@link #getCurrentUser()} at the start of operations requiring
 * ownership checks or current-user context. This avoids passing userId through
 * every method signature and prevents ID spoofing from request bodies.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Returns the {@link User} entity for the currently authenticated principal.
     *
     * @throws ResourceNotFoundException if the user email from the token no longer exists in DB
     */
    public User getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));
    }

    /**
     * Returns the email of the currently authenticated user.
     */
    public String getCurrentUserEmail() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }
}
