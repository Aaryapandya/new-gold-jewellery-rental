package com.goldrental.repository;

import com.goldrental.domain.entity.Jewellery;
import com.goldrental.domain.enums.JewelleryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JewelleryRepository extends JpaRepository<Jewellery, Long> {

    Page<Jewellery> findAllBySupplierId(Long supplierId, Pageable pageable);

    Page<Jewellery> findAllByStatus(JewelleryStatus status, Pageable pageable);

    /**
     * Radius-based jewellery discovery using the Haversine formula.
     *
     * <p>Only returns items that are:
     * <ul>
     *   <li>ACTIVE status (admin-approved)</li>
     *   <li>isAvailable = true (no active/approved booking)</li>
     *   <li>Within the specified radius</li>
     * </ul>
     *
     * <p>Optionally filters by category.
     *
     * <p>Haversine formula used directly in JPQL via native query for performance.
     * Earth radius used: 6371 km.
     */
    @Query(value = """
            SELECT j.*,
                   (6371 * acos(
                       cos(radians(:lat)) * cos(radians(j.latitude))
                       * cos(radians(j.longitude) - radians(:lng))
                       + sin(radians(:lat)) * sin(radians(j.latitude))
                   )) AS distance_km
            FROM jewellery j
            WHERE j.status = 'ACTIVE'
              AND j.is_available = TRUE
              AND (:category IS NULL OR j.category = :category)
              AND (6371 * acos(
                       cos(radians(:lat)) * cos(radians(j.latitude))
                       * cos(radians(j.longitude) - radians(:lng))
                       + sin(radians(:lat)) * sin(radians(j.latitude))
                   )) <= :radiusKm
            ORDER BY distance_km ASC
            """,
            countQuery = """
            SELECT COUNT(j.id)
            FROM jewellery j
            WHERE j.status = 'ACTIVE'
              AND j.is_available = TRUE
              AND (:category IS NULL OR j.category = :category)
              AND (6371 * acos(
                       cos(radians(:lat)) * cos(radians(j.latitude))
                       * cos(radians(j.longitude) - radians(:lng))
                       + sin(radians(:lat)) * sin(radians(j.latitude))
                   )) <= :radiusKm
            """,
            nativeQuery = true)
    Page<Jewellery> findNearbyAvailableJewellery(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("category") String category,
            Pageable pageable);

    /**
     * Admin query: fetch all jewellery with optional status filter.
     */
    @Query("""
            SELECT j FROM Jewellery j
            WHERE (:status IS NULL OR j.status = :status)
            ORDER BY j.createdAt DESC
            """)
    Page<Jewellery> findAllWithOptionalStatus(@Param("status") JewelleryStatus status, Pageable pageable);

    List<Jewellery> findAllBySupplierIdAndStatus(Long supplierId, JewelleryStatus status);
}
