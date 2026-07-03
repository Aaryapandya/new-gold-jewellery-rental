package com.goldrental.controller;

import com.goldrental.dto.request.CreateJewelleryRequest;
import com.goldrental.dto.request.NearbySearchRequest;
import com.goldrental.dto.request.UpdateJewelleryRequest;
import com.goldrental.dto.response.ApiResponse;
import com.goldrental.dto.response.JewelleryResponse;
import com.goldrental.dto.response.PagedResponse;
import com.goldrental.service.JewelleryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST endpoints for jewellery listing management and discovery.
 *
 * <pre>
 * POST   /api/v1/jewellery                        – create listing   (SUPPLIER)
 * PUT    /api/v1/jewellery/{id}                   – update listing   (SUPPLIER, owner only)
 * DELETE /api/v1/jewellery/{id}                   – delete listing   (SUPPLIER, owner only)
 * GET    /api/v1/jewellery/{id}                   – get detail       (public)
 * GET    /api/v1/jewellery/my                     – supplier's own   (SUPPLIER)
 * GET    /api/v1/jewellery/nearby                 – radius search    (public)
 * POST   /api/v1/jewellery/{id}/images            – upload images    (SUPPLIER)
 * POST   /api/v1/jewellery/{id}/bills             – upload bills     (SUPPLIER)
 * </pre>
 */
@RestController
@RequestMapping("/jewellery")
@RequiredArgsConstructor
@Slf4j
public class JewelleryController {

    private final JewelleryService jewelleryService;

    // ─── SUPPLIER: Create ──────────────────────────────────────

    /**
     * Creates a new jewellery listing. Restricted to SUPPLIER role.
     * New listings start in UNDER_VERIFICATION status.
     *
     * <p>Sample request:
     * <pre>{@code
     * POST /api/v1/jewellery
     * Authorization: Bearer <supplier-token>
     * {
     *   "title": "22K Gold Necklace Set",
     *   "description": "Traditional bridal necklace with matching earrings",
     *   "weightInGrams": 45.5,
     *   "rentPerDay": 500.00,
     *   "category": "Necklace",
     *   "addressLine": "12, MG Road",
     *   "city": "Bangalore",
     *   "pincode": "560001",
     *   "latitude": 12.9716,
     *   "longitude": 77.5946
     * }
     * }</pre>
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<JewelleryResponse>> createJewellery(
            @Valid @RequestBody final CreateJewelleryRequest request) {

        final JewelleryResponse response = jewelleryService.createJewellery(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Jewellery listing created successfully", response));
    }

    // ─── SUPPLIER: Update ──────────────────────────────────────

    /**
     * Partially updates a jewellery listing. Only the owning supplier may call this.
     *
     * <p>Sample request:
     * <pre>{@code
     * PUT /api/v1/jewellery/1
     * Authorization: Bearer <supplier-token>
     * {
     *   "rentPerDay": 600.00,
     *   "description": "Updated description"
     * }
     * }</pre>
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<JewelleryResponse>> updateJewellery(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateJewelleryRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Jewellery updated", jewelleryService.updateJewellery(id, request)));
    }

    // ─── SUPPLIER: Delete ──────────────────────────────────────

    /**
     * Deletes a jewellery listing. Blocked if an ACTIVE or APPROVED booking exists.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<Void>> deleteJewellery(
            @PathVariable final Long id) {

        jewelleryService.deleteJewellery(id);
        return ResponseEntity.ok(ApiResponse.success("Jewellery listing deleted", null));
    }

    // ─── Public: Get by ID ─────────────────────────────────────

    /**
     * Returns the full details of a specific jewellery listing.
     * No authentication required.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JewelleryResponse>> getJewelleryById(
            @PathVariable final Long id) {

        return ResponseEntity.ok(ApiResponse.success(jewelleryService.getJewelleryById(id)));
    }

    // ─── SUPPLIER: My listings ─────────────────────────────────

    /**
     * Returns all jewellery listings created by the authenticated supplier.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code page} – 0-based page index (default: 0)</li>
     *   <li>{@code size} – page size (default: 20)</li>
     *   <li>{@code sort} – sort field,direction e.g. {@code createdAt,desc}</li>
     * </ul>
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<PagedResponse<JewelleryResponse>>> getMyJewellery(
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "createdAt") final String sortBy,
            @RequestParam(defaultValue = "desc") final String sortDir) {

        final Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(jewelleryService.getMyJewellery(pageable)));
    }

    // ─── Public: Nearby search ─────────────────────────────────

    /**
     * Discovers available jewellery within a radius of the given coordinates.
     * Only ACTIVE, available listings are returned, ordered by distance.
     *
     * <p>Sample request:
     * <pre>
     * GET /api/v1/jewellery/nearby?latitude=12.9716&amp;longitude=77.5946&amp;radiusKm=25&amp;category=Necklace
     * </pre>
     *
     * <p>Sample response item:
     * <pre>{@code
     * {
     *   "id": 1,
     *   "title": "22K Gold Necklace Set",
     *   "distanceKm": 3.42,
     *   ...
     * }
     * }</pre>
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PagedResponse<JewelleryResponse>>> getNearbyJewellery(
            @Valid @ModelAttribute final NearbySearchRequest searchRequest,
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size) {

        // Nearby results are ordered by distance ASC in the DB query — no explicit sort here
        final Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success(jewelleryService.getNearbyJewellery(searchRequest, pageable)));
    }

    // ─── SUPPLIER: Upload images ───────────────────────────────

    /**
     * Uploads one or more images for a jewellery listing.
     * Each file is stored in Cloudinary; the URLs are persisted to jewellery_images.
     *
     * <p>Sample request (multipart/form-data):
     * <pre>
     * POST /api/v1/jewellery/1/images
     * Authorization: Bearer &lt;supplier-token&gt;
     * Form field: files = [file1.jpg, file2.jpg]
     * </pre>
     */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<JewelleryResponse>> uploadImages(
            @PathVariable final Long id,
            @RequestParam("files") final List<MultipartFile> files) {

        return ResponseEntity.ok(
                ApiResponse.success("Images uploaded", jewelleryService.uploadImages(id, files)));
    }

    // ─── SUPPLIER: Upload bills ────────────────────────────────

    /**
     * Uploads purchase bill / invoice documents for a jewellery listing.
     * Bills serve as proof of ownership for admin verification.
     *
     * <p>Sample request (multipart/form-data):
     * <pre>
     * POST /api/v1/jewellery/1/bills
     * Authorization: Bearer &lt;supplier-token&gt;
     * Form field: files = [invoice.pdf]
     * </pre>
     */
    @PostMapping(value = "/{id}/bills", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<JewelleryResponse>> uploadBills(
            @PathVariable final Long id,
            @RequestParam("files") final List<MultipartFile> files) {

        return ResponseEntity.ok(
                ApiResponse.success("Bills uploaded", jewelleryService.uploadBills(id, files)));
    }

    // ─── Private helpers ───────────────────────────────────────

    private Pageable buildPageable(
            final int page, final int size, final String sortBy, final String sortDir) {
        final Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }
}
