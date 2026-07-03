package com.goldrental.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.goldrental.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Handles all file upload and deletion operations via Cloudinary.
 *
 * <p>Guideline: FileSystem::IoExceptionHandling — all IOExceptions are caught
 * and rethrown as {@link FileUploadException} with context.
 * Guideline: FileSystem::LargeFileHandling — file size is validated by Spring's
 * multipart config (max 10 MB per file, 50 MB per request).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private static final String JEWELLERY_FOLDER = "gold_rental/jewellery";
    private static final String DOCUMENTS_FOLDER = "gold_rental/documents";
    private static final String PROFILES_FOLDER  = "gold_rental/profiles";

    private final Cloudinary cloudinary;

    /**
     * Uploads a jewellery image to Cloudinary.
     *
     * @param file        the multipart file
     * @param jewelleryId ID of the jewellery (used as folder tag)
     * @return the secure HTTPS URL of the uploaded image
     */
    public String uploadJewelleryImage(final MultipartFile file, final Long jewelleryId) {
        return upload(file, JEWELLERY_FOLDER + "/item_" + jewelleryId, "image");
    }

    /**
     * Uploads a jewellery bill document to Cloudinary.
     *
     * @param file        the multipart file (PDF or image)
     * @param jewelleryId ID of the jewellery
     * @return the secure HTTPS URL of the uploaded document
     */
    public String uploadJewelleryBill(final MultipartFile file, final Long jewelleryId) {
        return upload(file, JEWELLERY_FOLDER + "/bills_" + jewelleryId, "raw");
    }

    /**
     * Uploads a user identity document.
     *
     * @param file   the multipart file
     * @param userId the owner's user ID
     * @param label  a descriptive label (e.g., "aadhaar_front", "pan_back")
     * @return the secure HTTPS URL
     */
    public String uploadDocument(final MultipartFile file, final Long userId, final String label) {
        return upload(file, DOCUMENTS_FOLDER + "/user_" + userId + "/" + label, "raw");
    }

    /**
     * Uploads a user profile photo.
     *
     * @param file   the multipart file
     * @param userId the owner's user ID
     * @return the secure HTTPS URL
     */
    public String uploadProfilePhoto(final MultipartFile file, final Long userId) {
        return upload(file, PROFILES_FOLDER + "/user_" + userId, "image");
    }

    /**
     * Deletes a file from Cloudinary by its public ID (extracted from the URL).
     *
     * @param publicId the Cloudinary public ID
     */
    public void deleteFile(final String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted file from Cloudinary: {}", publicId);
        } catch (final IOException ex) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, ex);
            // Non-fatal: we still proceed even if remote cleanup fails
        }
    }

    // ─── Private ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String upload(final MultipartFile file, final String folder, final String resourceType) {
        validateFile(file);
        try {
            final Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", resourceType,
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            );
            final Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            final String secureUrl = (String) result.get("secure_url");
            log.info("Uploaded file to Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (final IOException ex) {
            log.error("Cloudinary upload failed for folder [{}]: {}", folder, ex.getMessage(), ex);
            throw new FileUploadException("Failed to upload file to storage", ex);
        }
    }

    private void validateFile(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("Cannot upload an empty file");
        }
        // Additional content-type validation can be added here
    }
}
