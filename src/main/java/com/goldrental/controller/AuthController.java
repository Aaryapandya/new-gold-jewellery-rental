package com.goldrental.controller;

import com.goldrental.dto.request.LoginRequest;
import com.goldrental.dto.request.RegisterRequest;
import com.goldrental.dto.response.ApiResponse;
import com.goldrental.dto.response.AuthResponse;
import com.goldrental.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints — public (no JWT required).
 *
 * <pre>
 * POST /api/v1/auth/register   – create a new BUYER or SUPPLIER account
 * POST /api/v1/auth/login      – obtain a JWT access token
 * </pre>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user account.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/auth/register
     * {
     *   "name": "Priya Sharma",
     *   "email": "priya@example.com",
     *   "password": "P@ssword1",
     *   "mobileNumber": "9876543210",
     *   "role": "BUYER"
     * }
     * }</pre>
     *
     * <p>Sample response (201 Created):
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Registration successful",
     *   "data": {
     *     "accessToken": "eyJhbGci...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 86400000,
     *     "userId": 1,
     *     "email": "priya@example.com",
     *     "name": "Priya Sharma",
     *     "role": "BUYER"
     *   }
     * }
     * }</pre>
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody final RegisterRequest request) {

        log.debug("Registration request received for email: {}", request.getEmail());
        final AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    /**
     * Authenticate and receive a JWT access token.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/auth/login
     * {
     *   "email": "priya@example.com",
     *   "password": "P@ssword1"
     * }
     * }</pre>
     *
     * <p>Sample response (200 OK):
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "data": {
     *     "accessToken": "eyJhbGci...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 86400000,
     *     "userId": 1,
     *     "email": "priya@example.com",
     *     "name": "Priya Sharma",
     *     "role": "BUYER"
     *   }
     * }
     * }</pre>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody final LoginRequest request) {

        log.debug("Login request received for email: {}", request.getEmail());
        final AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
