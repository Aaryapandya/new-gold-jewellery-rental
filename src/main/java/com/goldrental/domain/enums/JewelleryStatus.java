package com.goldrental.domain.enums;

/**
 * Lifecycle states for a jewellery listing.
 *
 * <p>UNDER_VERIFICATION – newly created, pending admin review.</p>
 * <p>ACTIVE             – verified and publicly visible.</p>
 * <p>INACTIVE           – supplier-deactivated; hidden from search.</p>
 */
public enum JewelleryStatus {
    UNDER_VERIFICATION,
    ACTIVE,
    INACTIVE
}
