package com.glucobite.health.service;

import com.glucobite.auth.exception.InvalidAllergenException;
import com.glucobite.health.dto.HealthProfileResponse;
import com.glucobite.health.dto.HealthProfileUpdateRequest;
import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.AllergenRepository;
import com.glucobite.health.repository.HealthProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class HealthProfileService {

    private final HealthProfileRepository healthProfileRepository;
    private final AllergenRepository allergenRepository;

    public HealthProfileService(
            HealthProfileRepository healthProfileRepository,
            AllergenRepository allergenRepository
    ) {
        this.healthProfileRepository = healthProfileRepository;
        this.allergenRepository = allergenRepository;
    }

    @Transactional(readOnly = true)
    public HealthProfileResponse getProfile(Long userId) {
        return HealthProfileResponse.from(findProfile(userId));
    }

    @Transactional
    public HealthProfileResponse updateProfile(Long userId, HealthProfileUpdateRequest request) {
        HealthProfile profile = findProfile(userId);
        List<Allergen> allergens = findAllergens(request.allergenIds());
        profile.update(
                request.birthDate(),
                request.height(),
                request.weight(),
                request.sex(),
                request.healthGoal(),
                request.glucoseDeviceConnected(),
                request.dailyCarbsTarget(),
                request.vegetarianType(),
                normalizeNote(request.dietaryRestrictionNote()),
                allergens
        );

        return HealthProfileResponse.from(healthProfileRepository.saveAndFlush(profile));
    }

    private HealthProfile findProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .orElseThrow(HealthProfileNotFoundException::new);
    }

    private List<Allergen> findAllergens(Set<Long> allergenIds) {
        List<Allergen> allergens = allergenRepository.findAllById(allergenIds);
        if (allergens.size() != allergenIds.size()) {
            throw new InvalidAllergenException();
        }
        return allergens;
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }
}
