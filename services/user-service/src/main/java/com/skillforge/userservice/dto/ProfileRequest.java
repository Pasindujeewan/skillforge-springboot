package com.skillforge.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(

        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$")
        String username,

        @NotBlank
        @Size(max = 80)
        String displayName,

        @Size(max = 500)
        String bio,

        @Size(max = 500)
        String avatarUrl

) {
}
