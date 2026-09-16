package com.goldrental.service;

import com.goldrental.config.AppProperties;
import com.goldrental.domain.entity.Jewellery;
import com.goldrental.domain.entity.JewelleryBill;
import com.goldrental.domain.entity.JewelleryImage;
import com.goldrental.domain.entity.User;
import com.goldrental.domain.enums.BookingStatus;
import com.goldrental.domain.enums.JewelleryStatus;
import com.goldrental.dto.request.CreateJewelleryRequest;
import com.goldrental.dto.request.NearbySearchRequest;
import com.goldrental.dto.request.UpdateJewelleryRequest;
import com.goldrental.dto.response.JewelleryResponse;
import com.goldrental.dto.response.PagedResponse;
import com.goldrental.exception.AccessDeniedException;
import com.goldrental.exception.BusinessException;
import com.goldrental.exception.ResourceNotFoundException;
import com.goldrental.repository.BookingRepository;
import com.goldrental.repository.JewelleryRepository;
import com.goldrental.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Business logic for jewellery listings.
 *
 * <p>Key invariants enforced here:
 * <ul>
 *   <li>Only the owning supplier may update or delete their listing.</li>
 *   <li>A listing cannot be deleted while it has an APPROVED or ACTIVE booking.</li>
 *   <li>Nearby search only surfaces ACTIVE, available listings.</li>
 *   <li>Availability flag is toggled atomically during booking lifecycle (by BookingService).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JewelleryService {

    private final JewelleryRepository jewelleryRepository;
    private final BookingRepository   bookingRepository;
    private final SecurityUtils       securityUtils;
    private final StorageService      storageService;
    private final AppProperties       appProperties;

    // ─── Create ────────────────────────────────────────────────

    /**
     * Creates a new jewellery listing for the currently authenticated supplier.
     * New listings start in UNDER_VERIFICATION status and must be approved by admin
     * before appearing in search results.
     */
    @Transactional
    public JewelleryResponse createJewellery(final CreateJewelleryRequest req) {
        final User supplier = securityUtils.getCurrentUser();

        final Jewellery jewellery = Jewellery.builder()
                .supplier(supplier)
                .title(req.getTitle())
                .description(req.getDescription())
                .weightInGrams(req.getWeightInGrams())
                .rentPerDay(req.getRentPerDay())
                .category(req.getCategory())
                .addressLine(req.getAddressLine())
                .city(req.getCity())
                .pincode(req.getPincode())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .status(JewelleryStatus.UNDER_VERIFICATION)
                .isAvailable(true)
                .build();

        final Jewellery saved = jewelleryRepository.save(jewellery);
        log.info("Jewellery created: id={}, supplierId={}, title={}", saved.getId(), supplier.getId(), saved.getTitle());
        return toJewelleryResponse(saved, null);
    }

    // ─── Update ────────────────────────────────────────────────

    /**
     * Updates a jewellery listing. Only the owning supplier may do this.
     * Partial update: only non-null fields in the request are applied.
     *
     * <p>If location fields are changed, the listing is reset to UNDER_VERIFICATION
     * so that admin can re-verify the updated details.
     */
    @Transactional
    public JewelleryResponse updateJewellery(final Long jewelleryId, final UpdateJewelleryRequest req) {
        final User supplier = securityUtils.getCurrentUser();
        final Jewellery jewellery = loadJewellery(jewelleryId);
        assertOwnership(jewellery, supplier);

        boolean locationChanged = false;

        if (req.getTitle() != null)       jewellery.setTitle(req.getTitle());
        if (req.getDescription() != null) jewellery.setDescription(req.getDescription());
        if (req.getWeightInGrams() != null) jewellery.setWeightInGrams(req.getWeightInGrams());
        if (req.getRentPerDay() != null)  jewellery.setRentPerDay(req.getRentPerDay());
        if (req.getCategory() != null)    jewellery.setCategory(req.getCategory());
        if (req.getAddressLine() != null) jewellery.setAddressLine(req.getAddressLine());
        if (req.getCity() != null)        jewellery.setCity(req.getCity());
        if (req.getPincode() != null)     jewellery.setPincode(req.getPincode());

        if (req.getLatitude() != null) {
            jewellery.setLatitude(req.getLatitude());
            locationChanged = true;
        }
        if (req.getLongitude() != null) {
            jewellery.setLongitude(req.getLongitude());
            locationChanged = true;
        }

        // Re-submit for verification if location changed
        if (locationChanged && jewellery.getStatus() == JewelleryStatus.ACTIVE) {
            jewellery.setStatus(JewelleryStatus.UNDER_VERIFICATION);
            log.info("Jewellery id={} reset to UNDER_VERIFICATION due to location change", jewelleryId);
        }

        final Jewellery updated = jewelleryRepository.save(jewellery);
        log.info("Jewellery updated: id={}, supplierId={}", jewelleryId, supplier.getId());
        return toJewelleryResponse(updated, null);
    }

    // ─── Delete ────────────────────────────────────────────────

    /**
     * Permanently deletes a jewellery listing.
     *
     * <p>Edge cases blocked:
     * <ul>
     *   <li>Non-owner supplier cannot delete.</li>
     *   <li>Cannot delete while an APPROVED or ACTIVE booking exists (data integrity).</li>
     * </ul>
     */
    @Transactional
    public void deleteJewellery(final Long jewelleryId) {
        final User supplier = securityUtils.getCurrentUser();
        final Jewellery jewellery = loadJewellery(jewelleryId);
        assertOwnership(jewellery, supplier);

        // Guard: active/approved bookings exist
        final boolean hasActiveBooking =
                bookingRepository.findFirstByJewelleryIdAndStatus(jewelleryId, BookingStatus.ACTIVE).isPresent() ||
                bookingRepository.findFirstByJewelleryIdAndStatus(jewelleryId, BookingStatus.APPROVED).isPresent();

        if (hasActiveBooking) {
            throw new BusinessException(
                    "Cannot delete jewellery with an active or approved booking. " +
                    "Complete or cancel the booking first.");
        }

        jewelleryRepository.delete(jewellery);
        log.info("Jewellery deleted: id={}, supplierId={}", jewelleryId, supplier.getId());
    }

    // ─── Read ──────────────────────────────────────────────────

    /**
     * Returns the full details of a specific jewellery item.
     * Publicly accessible (no authentication required).
     *
     * <p>Returns 404 if the owning supplier is inactive, so deactivated
     * suppliers' listings are never visible to buyers.
     */
    @Transactional(readOnly = true)
    public JewelleryResponse getJewelleryById(final Long jewelleryId) {
        final Jewellery jewellery = loadJewellery(jewelleryId);
        if (!jewellery.getSupplier().isActive()) {
            throw new ResourceNotFoundException("Jewellery", jewelleryId);
        }
        return toJewelleryResponse(jewellery, null);
    }

    /**
     * Returns all jewellery listings owned by the currently authenticated supplier, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<JewelleryResponse> getMyJewellery(final Pageable pageable) {
        final User supplier = securityUtils.getCurrentUser();
        final Page<Jewellery> page = jewelleryRepository.findAllBySupplierId(supplier.getId(), pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(j -> toJewelleryResponse(j, null))
                .toList());
    }

    /**
     * Location-based jewellery discovery using the Haversine formula.
     *
     * <p>Only ACTIVE, available listings within the requested radius are returned.
     * Radius is capped at the configured maximum to prevent full-table scans.
     */
    @Transactional(readOnly = true)
    public PagedResponse<JewelleryResponse> getNearbyJewellery(
            final NearbySearchRequest req, final Pageable pageable) {

        final double radius = resolveRadius(req.getRadiusKm());

        final Page<Jewellery> page = jewelleryRepository.findNearbyAvailableJewellery(
                req.getLatitude(),
                req.getLongitude(),
                radius,
                req.getCategory(),
                pageable);

        final List<JewelleryResponse> responses = page.getContent().stream()
                .map(j -> {
                    final double dist = haversineKm(
                            req.getLatitude(), req.getLongitude(),
                            j.getLatitude(),   j.getLongitude());
                    return toJewelleryResponse(j, dist);
                })
                .toList();

        return PagedResponse.of(page, responses);
    }

    // ─── Media uploads ─────────────────────────────────────────

    /**
     * Uploads one or more images for a jewellery listing.
     * Each file is uploaded to Cloudinary and a new {@link JewelleryImage} row is appended.
     */
    @Transactional
    public JewelleryResponse uploadImages(final Long jewelleryId, final List<MultipartFile> files) {
        final User supplier = securityUtils.getCurrentUser();
        final Jewellery jewellery = loadJewellery(jewelleryId);
        assertOwnership(jewellery, supplier);

        int nextOrder = jewellery.getImages().size();
        for (final MultipartFile file : files) {
            final String url = storageService.uploadJewelleryImage(file, jewelleryId);
            final JewelleryImage image = JewelleryImage.builder()
                    .jewellery(jewellery)
                    .imageUrl(url)
                    .displayOrder(nextOrder++)
                    .build();
            jewellery.getImages().add(image);
        }

        log.info("Uploaded {} image(s) to jewellery id={}", files.size(), jewelleryId);
        return toJewelleryResponse(jewelleryRepository.save(jewellery), null);
    }

    /**
     * Uploads one or more bill documents for a jewellery listing (proof of ownership).
     */
    @Transactional
    public JewelleryResponse uploadBills(final Long jewelleryId, final List<MultipartFile> files) {
        final User supplier = securityUtils.getCurrentUser();
        final Jewellery jewellery = loadJewellery(jewelleryId);
        assertOwnership(jewellery, supplier);

        for (final MultipartFile file : files) {
            final String url = storageService.uploadJewelleryBill(file, jewelleryId);
            final JewelleryBill bill = JewelleryBill.builder()
                    .jewellery(jewellery)
                    .billUrl(url)
                    .build();
            jewellery.getBills().add(bill);
        }

        log.info("Uploaded {} bill(s) to jewellery id={}", files.size(), jewelleryId);
        return toJewelleryResponse(jewelleryRepository.save(jewellery), null);
    }

    // ─── Admin operations ──────────────────────────────────────

    /**
     * Admin: changes the lifecycle status of a jewellery listing.
     * Deactivating an ACTIVE listing (setting INACTIVE) resets the availability flag
     * to prevent new bookings until it is re-activated.
     */
    @Transactional
    public JewelleryResponse changeJewelleryStatus(final Long jewelleryId, final JewelleryStatus newStatus) {
        final Jewellery jewellery = loadJewellery(jewelleryId);
        final JewelleryStatus oldStatus = jewellery.getStatus();
        jewellery.setStatus(newStatus);

        // If deactivated while available, mark unavailable to prevent new bookings
        if (newStatus == JewelleryStatus.INACTIVE && jewellery.isAvailable()) {
            jewellery.setAvailable(false);
        }
        // If re-activated from INACTIVE, restore availability
        if (newStatus == JewelleryStatus.ACTIVE && oldStatus == JewelleryStatus.INACTIVE) {
            jewellery.setAvailable(true);
        }

        log.info("Admin changed jewellery id={} status: {} → {}", jewelleryId, oldStatus, newStatus);
        return toJewelleryResponse(jewelleryRepository.save(jewellery), null);
    }

    /**
     * Admin: retrieves all jewellery listings with optional status filter.
     */
    @Transactional(readOnly = true)
    public PagedResponse<JewelleryResponse> getAllJewellery(
            final JewelleryStatus status, final Pageable pageable) {
        final Page<Jewellery> page = jewelleryRepository.findAllWithOptionalStatus(status, pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(j -> toJewelleryResponse(j, null))
                .toList());
    }

    // ─── Package-private helpers (used by BookingService) ──────

    /**
     * Loads a jewellery entity by ID. Used by external callers (e.g. {@link BookingService})
     * via the Spring proxy — hence the {@code @Transactional} annotation is effective here.
     */
    @Transactional(readOnly = true)
    public Jewellery findJewelleryOrThrow(final Long jewelleryId) {
        return loadJewellery(jewelleryId);
    }

    /**
     * Atomically updates the {@code isAvailable} flag. Called by {@link BookingService}
     * on booking status transitions to keep availability consistent.
     */
    @Transactional
    public void setAvailability(final Long jewelleryId, final boolean available) {
        final Jewellery jewellery = loadJewellery(jewelleryId);
        jewellery.setAvailable(available);
        jewelleryRepository.save(jewellery);
        log.debug("Jewellery id={} availability set to {}", jewelleryId, available);
    }

    /**
     * Non-transactional private loader — safe to call within existing transactions.
     * Avoids the Spring proxy self-invocation warning that arises from calling
     * a {@code @Transactional} method via {@code this}.
     */
    private Jewellery loadJewellery(final Long jewelleryId) {
        return jewelleryRepository.findById(jewelleryId)
                .orElseThrow(() -> new ResourceNotFoundException("Jewellery", jewelleryId));
    }

    // ─── Mapper ────────────────────────────────────────────────

    public JewelleryResponse toJewelleryResponse(final Jewellery j, final Double distanceKm) {
        return JewelleryResponse.builder()
                .id(j.getId())
                .supplierId(j.getSupplier().getId())
                .supplierName(j.getSupplier().getName())
                .title(j.getTitle())
                .description(j.getDescription())
                .weightInGrams(j.getWeightInGrams())
                .rentPerDay(j.getRentPerDay())
                .category(j.getCategory())
                .addressLine(j.getAddressLine())
                .city(j.getCity())
                .pincode(j.getPincode())
                .latitude(j.getLatitude())
                .longitude(j.getLongitude())
                .isAvailable(j.isAvailable())
                .status(j.getStatus())
                .imageUrls(j.getImages().stream().map(JewelleryImage::getImageUrl).toList())
                .billUrls(j.getBills().stream().map(JewelleryBill::getBillUrl).toList())
                .createdAt(j.getCreatedAt())
                .distanceKm(distanceKm)
                .build();
    }

    // ─── Private helpers ───────────────────────────────────────

    private void assertOwnership(final Jewellery jewellery, final User supplier) {
        if (!jewellery.getSupplier().getId().equals(supplier.getId())) {
            throw new AccessDeniedException(
                    "You do not own jewellery with id " + jewellery.getId());
        }
    }

    private double resolveRadius(final Double requested) {
        if (requested == null) {
            return appProperties.getSearch().getDefaultRadiusKm();
        }
        return Math.min(requested, appProperties.getSearch().getMaxRadiusKm());
    }

    /**
     * Pure-Java Haversine formula — used to annotate search results with the
     * actual computed distance without an additional DB round-trip.
     *
     * @param lat1 source latitude
     * @param lon1 source longitude
     * @param lat2 target latitude
     * @param lon2 target longitude
     * @return great-circle distance in kilometres
     */
    private double haversineKm(
            final double lat1, final double lon1,
            final double lat2, final double lon2) {
        final double R = 6371.0;
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLon = Math.toRadians(lon2 - lon1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
