package com.glucobite.ingredient.repository;

import com.glucobite.ingredient.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Optional<Ingredient> findFirstByTitleIgnoreCase(String title);
}
