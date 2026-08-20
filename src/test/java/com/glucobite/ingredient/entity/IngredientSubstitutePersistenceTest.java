package com.glucobite.ingredient.entity;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IngredientSubstitutePersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsSubstituteWithRatioAndReason() {
        Ingredient pork = entityManager.persistAndFlush(new Ingredient("돼지고기"));
        Ingredient chicken = entityManager.persistAndFlush(new Ingredient("닭가슴살"));
        IngredientSubstitute substitute = new IngredientSubstitute(
                pork,
                chicken,
                new BigDecimal("0.8000"),
                "지방을 줄이기 위한 대체"
        );

        IngredientSubstitute saved = entityManager.persistFlushFind(substitute);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIngredient().getId()).isEqualTo(pork.getId());
        assertThat(saved.getSubstitute().getId()).isEqualTo(chicken.getId());
        assertThat(saved.getRatio()).isEqualByComparingTo("0.8000");
        assertThat(saved.getReason()).isEqualTo("지방을 줄이기 위한 대체");
    }

    @Test
    void rejectsDuplicateIngredientSubstitutePair() {
        Ingredient beef = entityManager.persistAndFlush(new Ingredient("쇠고기"));
        Ingredient tofu = entityManager.persistAndFlush(new Ingredient("두부"));
        entityManager.persistAndFlush(new IngredientSubstitute(
                beef,
                tofu,
                new BigDecimal("1.0000"),
                "동량 대체"
        ));

        assertThatThrownBy(() -> entityManager.persistAndFlush(new IngredientSubstitute(
                beef,
                tofu,
                new BigDecimal("0.5000"),
                "중복"
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    void databaseRejectsNonPositiveRatio() {
        Ingredient beef = entityManager.persistAndFlush(new Ingredient("쇠고기"));
        Ingredient tofu = entityManager.persistAndFlush(new Ingredient("두부"));

        assertThatThrownBy(() -> insertSubstitute(beef.getId(), tofu.getId(), "0.0000"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsSelfSubstitute() {
        Ingredient beef = entityManager.persistAndFlush(new Ingredient("쇠고기"));

        assertThatThrownBy(() -> insertSubstitute(beef.getId(), beef.getId(), "1.0000"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertSubstitute(Long ingredientId, Long substituteId, String ratio) {
        jdbcTemplate.update(
                """
                INSERT INTO ingredient_substitutes (
                    ingredient_id, substitute_ingredient_id, ratio, created_at, updated_at
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                ingredientId,
                substituteId,
                new BigDecimal(ratio)
        );
    }
}
