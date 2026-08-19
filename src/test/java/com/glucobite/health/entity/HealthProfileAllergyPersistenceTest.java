package com.glucobite.health.entity;

import com.glucobite.health.repository.AllergenRepository;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HealthProfileAllergyPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AllergenRepository allergenRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsSeedAllergens() {
        List<String> allergenNames = allergenRepository.findAll().stream()
                .map(Allergen::getName)
                .toList();

        assertEquals(19, allergenNames.size());
        assertTrue(allergenNames.containsAll(List.of("난류", "우유", "땅콩", "밀")));
    }

    @Test
    void persistsMultipleAllergiesForProfile() {
        User user = entityManager.persistAndFlush(
                new User("allergy-user", "encoded-password", "알레르기 사용자")
        );
        Allergen milk = allergenRepository.findByName("우유").orElseThrow();
        Allergen peanut = allergenRepository.findByName("땅콩").orElseThrow();
        HealthProfile profile = healthProfileRepository.saveAndFlush(createProfile(
                user,
                List.of(milk, peanut)
        ));
        entityManager.clear();

        HealthProfile savedProfile = healthProfileRepository.findById(profile.getId()).orElseThrow();
        Set<String> savedNames = savedProfile.getAllergens().stream()
                .map(Allergen::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("우유", "땅콩"), savedNames);
    }

    @Test
    void rejectsDuplicateProfileAllergy() {
        User user = entityManager.persistAndFlush(
                new User("duplicate-user", "encoded-password", "중복 사용자")
        );
        Allergen milk = allergenRepository.findByName("우유").orElseThrow();
        HealthProfile profile = healthProfileRepository.saveAndFlush(createProfile(user, List.of(milk)));

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO health_profile_allergies (profile_id, allergen_id) VALUES (?, ?)",
                        profile.getId(),
                        milk.getId()
                )
        );
    }

    private HealthProfile createProfile(User user, List<Allergen> allergens) {
        return new HealthProfile(
                user,
                LocalDate.of(2000, 1, 1),
                new BigDecimal("165.50"),
                new BigDecimal("55.20"),
                Sex.FEMALE,
                HealthGoal.CARB_MANAGEMENT,
                null,
                false,
                180,
                VegetarianType.NONE,
                null,
                allergens
        );
    }
}
