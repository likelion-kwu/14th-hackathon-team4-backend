package com.glucobite.ingredient.repository;

import com.glucobite.ingredient.entity.IngredientNutrition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IngredientNutritionRepository extends JpaRepository<IngredientNutrition, Long> {

    Optional<IngredientNutrition> findByIngredientId(Long ingredientId);

    List<IngredientNutrition> findByIngredientIdIn(Collection<Long> ingredientIds);
}
