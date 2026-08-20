package com.glucobite.ingredient.repository;

import com.glucobite.ingredient.entity.IngredientSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientSubstituteRepository extends JpaRepository<IngredientSubstitute, Long> {

    List<IngredientSubstitute> findByIngredientIdOrderByIdAsc(Long ingredientId);

    Optional<IngredientSubstitute> findByIngredientIdAndSubstituteId(
            Long ingredientId,
            Long substituteId
    );

}
