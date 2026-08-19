package com.glucobite.health.controller;

import com.glucobite.health.dto.HealthProfileResponse;
import com.glucobite.health.dto.HealthProfileUpdateRequest;
import com.glucobite.health.service.HealthProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/profile")
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    public HealthProfileController(HealthProfileService healthProfileService) {
        this.healthProfileService = healthProfileService;
    }

    @GetMapping
    public HealthProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return healthProfileService.getProfile(Long.valueOf(jwt.getSubject()));
    }

    @PutMapping
    public HealthProfileResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HealthProfileUpdateRequest request
    ) {
        return healthProfileService.updateProfile(Long.valueOf(jwt.getSubject()), request);
    }
}
