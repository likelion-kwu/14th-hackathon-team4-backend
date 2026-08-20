package com.glucobite.recipe.repository;

import com.glucobite.recipe.entity.RecipeSubstitutionSuggestionSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RecipeSubstitutionSuggestionSourceRepository
        extends JpaRepository<RecipeSubstitutionSuggestionSource, Long> {

    List<RecipeSubstitutionSuggestionSource>
    findBySuggestionIdInOrderBySuggestionIdAscSourceOrderAsc(Collection<Long> suggestionIds);
}
