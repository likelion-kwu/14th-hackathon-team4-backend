package com.glucobite.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 4, max = 100) String loginId,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 50) String nickname,
        @NotNull @Valid SignupProfileRequest profile
) {
}
