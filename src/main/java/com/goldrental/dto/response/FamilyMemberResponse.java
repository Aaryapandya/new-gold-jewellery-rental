package com.goldrental.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for a family member record.
 */
@Data
@Builder
public class FamilyMemberResponse {
    private Long id;
    private String name;
    private String relation;
    private Instant createdAt;
}
