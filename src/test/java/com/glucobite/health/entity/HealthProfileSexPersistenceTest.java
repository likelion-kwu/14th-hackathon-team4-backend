package com.glucobite.health.entity;

import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.user.entity.User;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HealthProfileSexPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @ParameterizedTest
    @EnumSource(Sex.class)
    void persistsSupportedSex(Sex sex) {
        User user = entityManager.persistAndFlush(new User(
                "sex-" + sex.name().toLowerCase(),
                "encoded-password",
                "성별 저장 사용자"
        ));
        HealthProfile profile = healthProfileRepository.saveAndFlush(new HealthProfile(
                user,
                LocalDate.of(2000, 1, 1),
                new BigDecimal("165.50"),
                new BigDecimal("55.20"),
                sex,
                HealthGoal.CARB_MANAGEMENT,
                null,
                false,
                180,
                VegetarianType.NONE,
                null,
                List.of()
        ));
        entityManager.clear();

        HealthProfile savedProfile = healthProfileRepository.findById(profile.getId()).orElseThrow();

        assertThat(savedProfile.getSex()).isEqualTo(sex);
    }
}
