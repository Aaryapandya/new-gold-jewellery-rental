package com.goldrental.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for adding a family member to a user's profile.
 */
@Data
public class FamilyMemberRequest {

    @NotBlank(message = "Family member name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Relation is required")
    @Size(max = 100)
    private String relation;
}
