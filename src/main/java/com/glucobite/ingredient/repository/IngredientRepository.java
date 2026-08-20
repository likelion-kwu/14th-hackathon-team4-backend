package com.glucobite.ingredient.repository;

import com.glucobite.ingredient.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Optional<Ingredient> findByNormalizedTitle(String normalizedTitle);

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO ingredients (title, normalized_title)
                    VALUES (:title, :normalizedTitle)
                    """,
            nativeQuery = true
    )
    int insertIgnore(
            @Param("title") String title,
            @Param("normalizedTitle") String normalizedTitle
    );
}
