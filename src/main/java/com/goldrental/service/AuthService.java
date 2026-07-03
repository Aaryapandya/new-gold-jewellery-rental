package com.goldrental.service;

import com.goldrental.domain.entity.User;
import com.goldrental.domain.enums.UserRole;
import com.goldrental.dto.request.LoginRequest;
import com.goldrental.dto.request.RegisterRequest;
import com.goldrental.dto.response.AuthResponse;
import com.goldrental.exception.BusinessException;
import com.goldrental.exception.DuplicateResourceException;
import com.goldrental.repository.UserRepository;
import com.goldrental.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(final RegisterRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new BusinessException("ADMIN accounts cannot be self-registered");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (request.getMobileNumber() != null
                && userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateResourceException(
                    "Mobile number already registered: " + request.getMobileNumber());
        }

        final User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .mobileNumber(request.getMobileNumber())
                .role(request.getRole())
                .build();

        final User saved = userRepository.save(user);
        log.info("User registered: id={}, email={}, role={}", saved.getId(), saved.getEmail(), saved.getRole());

        final String token = jwtTokenProvider.generateTokenFromEmail(saved.getEmail());
        return buildAuthResponse(saved, token);
    }

    public AuthResponse login(final LoginRequest request) {
        final Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        final String token = jwtTokenProvider.generateToken(auth);
        final User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(final User user, final String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }
}
