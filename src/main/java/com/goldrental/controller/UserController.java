package com.goldrental.controller;

import com.goldrental.dto.request.FamilyMemberRequest;
import com.goldrental.dto.request.UpdateProfileRequest;
import com.goldrental.dto.response.ApiResponse;
import com.goldrental.dto.response.FamilyMemberResponse;
import com.goldrental.dto.response.UserResponse;
import com.goldrental.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * User profile, document upload, and family-member management endpoints.
 *
 * <p>All routes require an authenticated JWT. The current user is resolved from
 * the security context — never from a path variable — to prevent IDOR attacks.
 *
 * <pre>
 * GET    /api/v1/users/me                     – get own profile
 * PUT    /api/v1/users/me                     – update own profile
 * POST   /api/v1/users/me/photo               – upload profile photo
 * POST   /api/v1/users/me/documents/{docType} – upload identity document
 * GET    /api/v1/users/me/family              – list family members
 * POST   /api/v1/users/me/family              – add family member
 * DELETE /api/v1/users/me/family/{memberId}   – remove family member
 * </pre>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ─── Profile ───────────────────────────────────────────────

    /**
     * Returns the full profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile()));
    }

    /**
     * Partially updates the authenticated user's profile.
     * Only non-null fields in the request body are applied.
     *
     * <p>Sample request:
     * <pre>{@code
     * PUT /api/v1/users/me
     * {
     *   "city": "Mumbai",
     *   "pincode": "400001",
     *   "latitude": 19.0760,
     *   "longitude": 72.8777
     * }
     * }</pre>
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody final UpdateProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully", userService.updateProfile(request)));
    }

    // ─── Document uploads ──────────────────────────────────────

    /**
     * Uploads the user's profile photo to Cloudinary.
     * Accepts: image/jpeg, image/png (enforced by Cloudinary upload preset).
     *
     * <p>Sample request (multipart/form-data):
     * <pre>
     * POST /api/v1/users/me/photo
     * Form field: file = &lt;image file&gt;
     * </pre>
     */
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfilePhoto(
            @RequestParam("file") final MultipartFile file) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile photo uploaded", userService.uploadProfilePhoto(file)));
    }

    /**
     * Uploads an identity document for the authenticated user.
     *
     * <p>{@code docType} must be one of:
     * <ul>
     *   <li>{@code aadhaar_front}</li>
     *   <li>{@code aadhaar_back}</li>
     *   <li>{@code pan_front}</li>
     *   <li>{@code pan_back}</li>
     * </ul>
     *
     * <p>Sample request (multipart/form-data):
     * <pre>
     * POST /api/v1/users/me/documents/aadhaar_front
     * Form field: file = &lt;document file&gt;
     * </pre>
     */
    @PostMapping(value = "/me/documents/{docType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadDocument(
            @PathVariable final String docType,
            @RequestParam("file") final MultipartFile file) {

        validateDocType(docType);
        return ResponseEntity.ok(
                ApiResponse.success("Document uploaded", userService.uploadDocument(file, docType)));
    }

    // ─── Family members ────────────────────────────────────────

    /**
     * Returns all family members associated with the authenticated user.
     */
    @GetMapping("/me/family")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> getFamilyMembers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getFamilyMembers()));
    }

    /**
     * Adds a new family member to the authenticated user's profile.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/users/me/family
     * {
     *   "name": "Ravi Sharma",
     *   "relation": "Brother"
     * }
     * }</pre>
     */
    @PostMapping("/me/family")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> addFamilyMember(
            @Valid @RequestBody final FamilyMemberRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Family member added", userService.addFamilyMember(request)));
    }

    /**
     * Removes a specific family member.
     * Only the owning user may delete their own family members.
     */
    @DeleteMapping("/me/family/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeFamilyMember(
            @PathVariable final Long memberId) {
        userService.removeFamilyMember(memberId);
        return ResponseEntity.ok(ApiResponse.success("Family member removed", null));
    }

    // ─── Private helpers ───────────────────────────────────────

    private void validateDocType(final String docType) {
        final List<String> allowed = List.of("aadhaar_front", "aadhaar_back", "pan_front", "pan_back");
        if (!allowed.contains(docType)) {
            throw new com.goldrental.exception.BusinessException(
                    "Invalid document type. Allowed: " + String.join(", ", allowed));
        }
    }
}
