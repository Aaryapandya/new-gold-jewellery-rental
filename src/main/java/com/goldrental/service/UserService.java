package com.goldrental.service;

import com.goldrental.domain.entity.FamilyMember;
import com.goldrental.domain.entity.User;
import com.goldrental.dto.request.FamilyMemberRequest;
import com.goldrental.dto.request.UpdateProfileRequest;
import com.goldrental.dto.response.FamilyMemberResponse;
import com.goldrental.dto.response.UserResponse;
import com.goldrental.exception.ResourceNotFoundException;
import com.goldrental.repository.FamilyMemberRepository;
import com.goldrental.repository.UserRepository;
import com.goldrental.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Business logic for user profile management, document uploads, and family members.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final SecurityUtils securityUtils;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return toUserResponse(securityUtils.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(final Long userId) {
        return toUserResponse(findUserOrThrow(userId));
    }

    @Transactional
    public UserResponse updateProfile(final UpdateProfileRequest req) {
        final User user = securityUtils.getCurrentUser();
        if (req.getName() != null)         user.setName(req.getName());
        if (req.getMobileNumber() != null) user.setMobileNumber(req.getMobileNumber());
        if (req.getAddress() != null)      user.setAddress(req.getAddress());
        if (req.getCity() != null)         user.setCity(req.getCity());
        if (req.getPincode() != null)      user.setPincode(req.getPincode());
        if (req.getLatitude() != null)     user.setLatitude(req.getLatitude());
        if (req.getLongitude() != null)    user.setLongitude(req.getLongitude());
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse uploadProfilePhoto(final MultipartFile file) {
        final User user = securityUtils.getCurrentUser();
        final String url = storageService.uploadProfilePhoto(file, user.getId());
        user.setProfilePhotoUrl(url);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse uploadDocument(final MultipartFile file, final String docType) {
        final User user = securityUtils.getCurrentUser();
        final String url = storageService.uploadDocument(file, user.getId(), docType);
        switch (docType) {
            case "aadhaar_front" -> user.setAadhaarFrontUrl(url);
            case "aadhaar_back"  -> user.setAadhaarBackUrl(url);
            case "pan_front"     -> user.setPanFrontUrl(url);
            case "pan_back"      -> user.setPanBackUrl(url);
            default -> throw new IllegalArgumentException("Unknown document type: " + docType);
        }
        log.info("Document [{}] uploaded for user id={}", docType, user.getId());
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public FamilyMemberResponse addFamilyMember(final FamilyMemberRequest req) {
        final User user = securityUtils.getCurrentUser();
        final FamilyMember member = FamilyMember.builder()
                .user(user)
                .name(req.getName())
                .relation(req.getRelation())
                .build();
        return toFamilyMemberResponse(familyMemberRepository.save(member));
    }

    @Transactional
    public void removeFamilyMember(final Long memberId) {
        final User user = securityUtils.getCurrentUser();
        final FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", memberId));
        if (!member.getUser().getId().equals(user.getId())) {
            throw new com.goldrental.exception.AccessDeniedException("Cannot delete another user's family member");
        }
        familyMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getFamilyMembers() {
        final User user = securityUtils.getCurrentUser();
        return familyMemberRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toFamilyMemberResponse)
                .toList();
    }

    // ─── Mappers ───────────────────────────────────────────────

    public UserResponse toUserResponse(final User user) {
        final List<FamilyMemberResponse> members = familyMemberRepository
                .findAllByUserId(user.getId())
                .stream()
                .map(this::toFamilyMemberResponse)
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .mobileVerified(user.isMobileVerified())
                .isActive(user.isActive())
                .address(user.getAddress())
                .city(user.getCity())
                .pincode(user.getPincode())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .aadhaarFrontUrl(user.getAadhaarFrontUrl())
                .aadhaarBackUrl(user.getAadhaarBackUrl())
                .panFrontUrl(user.getPanFrontUrl())
                .panBackUrl(user.getPanBackUrl())
                .familyMembers(members)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private FamilyMemberResponse toFamilyMemberResponse(final FamilyMember m) {
        return FamilyMemberResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .relation(m.getRelation())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private User findUserOrThrow(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
