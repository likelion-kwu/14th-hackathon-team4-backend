package com.glucobite.recipe.service;

import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.entity.IngredientSubstitute;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientRepository;
import com.glucobite.ingredient.repository.IngredientSubstituteRepository;
import com.glucobite.recipe.dto.GenerateIngredientAlternativesRequest;
import com.glucobite.recipe.dto.IngredientAlternativeSourceResponse;
import com.glucobite.recipe.dto.IngredientAlternativeSuggestionListResponse;
import com.glucobite.recipe.dto.IngredientAlternativeSuggestionOrigin;
import com.glucobite.recipe.dto.IngredientAlternativeSuggestionResponse;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.dto.RecipeIngredientResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.entity.RecipeStep;
import com.glucobite.recipe.entity.RecipeSubstitutionSuggestion;
import com.glucobite.recipe.entity.RecipeSubstitutionSuggestionSource;
import com.glucobite.recipe.exception.IngredientNotFoundException;
import com.glucobite.recipe.exception.InvalidSubstituteIngredientException;
import com.glucobite.recipe.exception.RecipeNotFoundException;
import com.glucobite.recipe.exception.RecipePersonalizationGenerationException;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import com.glucobite.recipe.repository.RecipeSubstitutionSuggestionRepository;
import com.glucobite.recipe.repository.RecipeSubstitutionSuggestionSourceRepository;
import com.glucobite.recipe.substitution.GeneratedSubstitutionSuggestions;
import com.glucobite.recipe.substitution.RecipeSubstitutionSuggestionGenerator;
import com.glucobite.recipe.substitution.SubstitutionSuggestionContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecipeSubstitutionSuggestionService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    private static final BigDecimal MAX_NUTRITION_PER_GRAM = new BigDecimal("99999999.999999");
    private static final int MAX_SUGGESTIONS = 3;

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final IngredientSubstituteRepository ingredientSubstituteRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final RecipeSubstitutionSuggestionRepository suggestionRepository;
    private final RecipeSubstitutionSuggestionSourceRepository sourceRepository;
    private final RecipeNutritionCalculator nutritionCalculator;
    private final DietaryRestrictionPolicy dietaryRestrictionPolicy;
    private final RecipeSubstitutionSuggestionGenerator generator;
    private final TransactionTemplate transactionTemplate;

    public RecipeSubstitutionSuggestionService(
            RecipeRepository recipeRepository,
            RecipeIngredientRepository recipeIngredientRepository,
            RecipeStepRepository recipeStepRepository,
            IngredientRepository ingredientRepository,
            IngredientNutritionRepository ingredientNutritionRepository,
            IngredientSubstituteRepository ingredientSubstituteRepository,
            HealthProfileRepository healthProfileRepository,
            RecipeSubstitutionSuggestionRepository suggestionRepository,
            RecipeSubstitutionSuggestionSourceRepository sourceRepository,
            RecipeNutritionCalculator nutritionCalculator,
            DietaryRestrictionPolicy dietaryRestrictionPolicy,
            RecipeSubstitutionSuggestionGenerator generator,
            TransactionTemplate transactionTemplate
    ) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientNutritionRepository = ingredientNutritionRepository;
        this.ingredientSubstituteRepository = ingredientSubstituteRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.suggestionRepository = suggestionRepository;
        this.sourceRepository = sourceRepository;
        this.nutritionCalculator = nutritionCalculator;
        this.dietaryRestrictionPolicy = dietaryRestrictionPolicy;
        this.generator = generator;
        this.transactionTemplate = transactionTemplate;
    }

    public IngredientAlternativeSuggestionListResponse generate(
            Long userId,
            Long recipeId,
            Long ingredientId,
            GenerateIngredientAlternativesRequest request
    ) {
        String userInput = request.userInput().trim().replaceAll("\\s+", " ");
        String requestKey = requestKey(userInput, request.excludeSuggestionIds());
        LoadedContext loaded = transactionTemplate.execute(status -> loadContext(
                userId,
                recipeId,
                ingredientId,
                userInput,
                request.excludeSuggestionIds(),
                requestKey
        ));
        if (loaded == null) {
            throw generationFailure("재료 대체 입력을 준비하지 못했습니다.");
        }
        if (loaded.cachedResponse() != null) {
            return loaded.cachedResponse();
        }
        if (loaded.registeredResponse() != null) {
            return loaded.registeredResponse();
        }

        GeneratedSubstitutionSuggestions generated = generator.generate(loaded.context());
        try {
            IngredientAlternativeSuggestionListResponse response = transactionTemplate.execute(status ->
                    persistGenerated(
                            userId,
                            recipeId,
                            ingredientId,
                            userInput,
                            requestKey,
                            loaded.restrictedTerms(),
                            loaded.originalContribution(),
                            generated
                    )
            );
            if (response == null) {
                throw generationFailure("재료 대체 후보를 저장하지 못했습니다.");
            }
            return response;
        } catch (DataIntegrityViolationException exception) {
            IngredientAlternativeSuggestionListResponse cached = transactionTemplate.execute(status ->
                    loadCachedResponse(userId, recipeId, ingredientId, requestKey)
            );
            if (cached != null) {
                return cached;
            }
            throw exception;
        }
    }

    private LoadedContext loadContext(
            Long userId,
            Long recipeId,
            Long ingredientId,
            String userInput,
            List<Long> excludeSuggestionIds,
            String requestKey
    ) {
        Recipe recipe = findOwnedRecipe(userId, recipeId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(HealthProfileNotFoundException::new);
        RecipeIngredient original = recipeIngredientRepository
                .findByRecipeIdAndIngredientId(recipeId, ingredientId)
                .orElseThrow(IngredientNotFoundException::new);
        IngredientNutrition originalFallback = ingredientNutritionRepository
                .findByIngredientId(ingredientId)
                .orElse(null);
        NutritionSummary originalContribution = nutritionCalculator.contribute(
                original,
                originalFallback
        );

        IngredientAlternativeSuggestionListResponse cached = loadCachedResponse(
                userId,
                recipeId,
                ingredientId,
                requestKey
        );
        if (cached != null) {
            return new LoadedContext(null, Set.of(), originalContribution, cached, null);
        }

        Set<String> restrictedTerms = dietaryRestrictionPolicy.restrictedTerms(profile);
        IngredientAlternativeSuggestionListResponse registered = registeredMatches(
                recipe,
                original,
                originalContribution,
                restrictedTerms,
                userInput
        );
        if (registered != null) {
            return new LoadedContext(null, restrictedTerms, originalContribution, null, registered);
        }

        List<RecipeSubstitutionSuggestion> excluded = excludeSuggestionIds.stream()
                .distinct()
                .map(id -> suggestionRepository
                        .findByIdAndUserIdAndRecipeIdAndOriginalIngredientId(
                                id,
                                userId,
                                recipeId,
                                ingredientId
                        )
                        .orElseThrow(InvalidSubstituteIngredientException::new))
                .toList();
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipeId);
        List<String> steps = recipeStepRepository.findByRecipeIdOrderByStepOrderAsc(recipeId).stream()
                .map(RecipeStep::getDescription)
                .toList();
        SubstitutionSuggestionContext context = new SubstitutionSuggestionContext(
                userInput,
                new SubstitutionSuggestionContext.RecipeData(
                        recipe.getTitle(),
                        recipe.getDescription(),
                        recipe.getCookingTime(),
                        recipeIngredients.stream()
                                .map(item -> new SubstitutionSuggestionContext.IngredientData(
                                        item.getIngredient().getId(),
                                        item.getIngredient().getTitle(),
                                        item.getAmount()
                                ))
                                .toList(),
                        steps
                ),
                new SubstitutionSuggestionContext.IngredientData(
                        original.getIngredient().getId(),
                        original.getIngredient().getTitle(),
                        original.getAmount()
                ),
                new SubstitutionSuggestionContext.HealthData(
                        profile.getHealthGoal().name(),
                        profile.getDiabetesStatus() == null ? null : profile.getDiabetesStatus().name(),
                        profile.getDailyCarbsTarget(),
                        profile.getVegetarianType().name(),
                        profile.getAllergens().stream().map(allergen -> allergen.getName()).toList(),
                        profile.getDietaryRestrictionNote()
                ),
                excluded.stream()
                        .map(item -> item.getSubstituteIngredient().getTitle())
                        .toList()
        );
        return new LoadedContext(context, restrictedTerms, originalContribution, null, null);
    }

    private IngredientAlternativeSuggestionListResponse registeredMatches(
            Recipe recipe,
            RecipeIngredient original,
            NutritionSummary originalContribution,
            Set<String> restrictedTerms,
            String userInput
    ) {
        String normalizedInput = compact(userInput);
        List<IngredientSubstitute> matches = ingredientSubstituteRepository
                .findByIngredientIdOrderByIdAsc(original.getIngredient().getId()).stream()
                .filter(item -> normalizedInput.contains(compact(item.getSubstitute().getTitle())))
                .filter(item -> !dietaryRestrictionPolicy.isRestricted(
                        item.getSubstitute(),
                        restrictedTerms
                ))
                .toList();
        if (matches.isEmpty()) {
            return null;
        }
        Map<Long, IngredientNutrition> nutritions = ingredientNutritionRepository
                .findByIngredientIdIn(matches.stream()
                        .map(item -> item.getSubstitute().getId())
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        nutrition -> nutrition.getIngredient().getId(),
                        Function.identity()
                ));
        List<IngredientAlternativeSuggestionResponse> suggestions = matches.stream()
                .map(item -> {
                    BigDecimal amount = original.getAmount()
                            .multiply(item.getRatio())
                            .setScale(2, RoundingMode.HALF_UP);
                    NutritionSummary contribution = nutritionCalculator.contribute(
                            nutritions.get(item.getSubstitute().getId()),
                            amount
                    );
                    return new IngredientAlternativeSuggestionResponse(
                            null,
                            item.getSubstitute().getId(),
                            IngredientAlternativeSuggestionOrigin.REGISTERED,
                            item.getSubstitute().getTitle(),
                            amount,
                            nutritionCalculator.changes(originalContribution, contribution),
                            item.getReason(),
                            null,
                            List.of()
                    );
                })
                .toList();
        return new IngredientAlternativeSuggestionListResponse(
                recipe.getId(),
                toIngredientResponse(original),
                suggestions
        );
    }

    private IngredientAlternativeSuggestionListResponse persistGenerated(
            Long userId,
            Long recipeId,
            Long ingredientId,
            String userInput,
            String requestKey,
            Set<String> restrictedTerms,
            NutritionSummary originalContribution,
            GeneratedSubstitutionSuggestions generated
    ) {
        Recipe recipe = findOwnedRecipe(userId, recipeId);
        RecipeIngredient original = recipeIngredientRepository
                .findByRecipeIdAndIngredientId(recipeId, ingredientId)
                .orElseThrow(IngredientNotFoundException::new);
        validateGenerated(generated, original, restrictedTerms);

        List<RecipeSubstitutionSuggestion> suggestions = new ArrayList<>();
        for (int index = 0; index < generated.suggestions().size(); index++) {
            GeneratedSubstitutionSuggestions.Suggestion generatedItem = generated.suggestions().get(index);
            Ingredient substitute = resolveIngredient(generatedItem.title());
            suggestions.add(new RecipeSubstitutionSuggestion(
                    recipe.getUser(),
                    recipe,
                    original.getIngredient(),
                    substitute,
                    requestKey,
                    index + 1,
                    userInput,
                    generatedItem.recommendedAmount().setScale(2, RoundingMode.HALF_UP),
                    generatedItem.reason().trim(),
                    trimNullable(generatedItem.warning()),
                    scaleNutrition(generatedItem.nutritionPerGram()),
                    trimNullable(generated.responseId())
            ));
        }
        suggestionRepository.saveAllAndFlush(suggestions);
        List<RecipeSubstitutionSuggestionSource> sources = new ArrayList<>();
        for (RecipeSubstitutionSuggestion suggestion : suggestions) {
            for (int index = 0; index < generated.sources().size(); index++) {
                GeneratedSubstitutionSuggestions.Source source = generated.sources().get(index);
                sources.add(new RecipeSubstitutionSuggestionSource(
                        suggestion,
                        index + 1,
                        source.title().trim(),
                        source.url().trim()
                ));
            }
        }
        sourceRepository.saveAll(sources);
        return toGeneratedResponse(
                recipe,
                original,
                originalContribution,
                suggestions,
                sources
        );
    }

    private IngredientAlternativeSuggestionListResponse loadCachedResponse(
            Long userId,
            Long recipeId,
            Long ingredientId,
            String requestKey
    ) {
        List<RecipeSubstitutionSuggestion> suggestions = suggestionRepository
                .findByUserIdAndRecipeIdAndOriginalIngredientIdAndRequestKeyOrderBySuggestionOrderAsc(
                        userId,
                        recipeId,
                        ingredientId,
                        requestKey
                );
        if (suggestions.isEmpty()) {
            return null;
        }
        Recipe recipe = suggestions.getFirst().getRecipe();
        RecipeIngredient original = recipeIngredientRepository
                .findByRecipeIdAndIngredientId(recipeId, ingredientId)
                .orElseThrow(IngredientNotFoundException::new);
        NutritionSummary originalContribution = nutritionCalculator.contribute(
                original,
                ingredientNutritionRepository.findByIngredientId(ingredientId).orElse(null)
        );
        List<RecipeSubstitutionSuggestionSource> sources = sourceRepository
                .findBySuggestionIdInOrderBySuggestionIdAscSourceOrderAsc(
                        suggestions.stream().map(RecipeSubstitutionSuggestion::getId).toList()
                );
        return toGeneratedResponse(recipe, original, originalContribution, suggestions, sources);
    }

    private IngredientAlternativeSuggestionListResponse toGeneratedResponse(
            Recipe recipe,
            RecipeIngredient original,
            NutritionSummary originalContribution,
            List<RecipeSubstitutionSuggestion> suggestions,
            List<RecipeSubstitutionSuggestionSource> sources
    ) {
        Map<Long, List<IngredientAlternativeSourceResponse>> sourcesBySuggestionId = sources.stream()
                .collect(Collectors.groupingBy(
                        source -> source.getSuggestion().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                source -> new IngredientAlternativeSourceResponse(
                                        source.getTitle(),
                                        source.getUrl()
                                ),
                                Collectors.toList()
                        )
                ));
        List<IngredientAlternativeSuggestionResponse> responseItems = suggestions.stream()
                .map(item -> {
                    NutritionSummary contribution = nutritionCalculator.contributePerGram(
                            item.nutritionPerGram(),
                            item.getRecommendedAmount()
                    );
                    return new IngredientAlternativeSuggestionResponse(
                            item.getId(),
                            item.getSubstituteIngredient().getId(),
                            IngredientAlternativeSuggestionOrigin.AI_WEB_SEARCH,
                            item.getSubstituteIngredient().getTitle(),
                            item.getRecommendedAmount(),
                            nutritionCalculator.changes(originalContribution, contribution),
                            item.getReason(),
                            item.getWarning(),
                            sourcesBySuggestionId.getOrDefault(item.getId(), List.of())
                    );
                })
                .toList();
        return new IngredientAlternativeSuggestionListResponse(
                recipe.getId(),
                toIngredientResponse(original),
                responseItems
        );
    }

    private void validateGenerated(
            GeneratedSubstitutionSuggestions generated,
            RecipeIngredient original,
            Set<String> restrictedTerms
    ) {
        if (generated == null
                || generated.suggestions() == null
                || generated.suggestions().isEmpty()
                || generated.suggestions().size() > MAX_SUGGESTIONS) {
            throw generationFailure("OpenAI 대체 후보는 1개에서 3개여야 합니다.");
        }
        if (generated.sources() == null || generated.sources().isEmpty()) {
            throw generationFailure("OpenAI 대체 후보의 web search 출처가 없습니다.");
        }
        Set<String> normalizedTitles = new HashSet<>();
        for (GeneratedSubstitutionSuggestions.Suggestion item : generated.suggestions()) {
            requireText(item.title(), 100, "대체 재료명");
            requireText(item.reason(), 500, "대체 근거");
            optionalText(item.warning(), 500, "조리상 주의점");
            if (Ingredient.normalizeTitle(item.title())
                    .equals(original.getIngredient().getNormalizedTitle())) {
                throw generationFailure("원본과 동일한 재료는 대체 후보가 될 수 없습니다.");
            }
            if (!normalizedTitles.add(Ingredient.normalizeTitle(item.title()))) {
                throw generationFailure("중복된 대체 후보를 사용할 수 없습니다.");
            }
            if (dietaryRestrictionPolicy.isRestricted(item.title(), restrictedTerms)) {
                throw generationFailure("건강 제한에 해당하는 대체 후보가 포함되었습니다.");
            }
            requirePositive(item.recommendedAmount(), MAX_AMOUNT, "권장 사용량");
            validateNutrition(item.nutritionPerGram());
        }
        for (GeneratedSubstitutionSuggestions.Source source : generated.sources()) {
            requireText(source.title(), 500, "출처 제목");
            requireText(source.url(), 1_000, "출처 URL");
            if (!source.url().startsWith("https://") && !source.url().startsWith("http://")) {
                throw generationFailure("출처 URL 형식이 올바르지 않습니다.");
            }
        }
    }

    private void validateNutrition(NutritionSummary nutrition) {
        if (nutrition == null) {
            throw generationFailure("대체 후보의 영양정보가 없습니다.");
        }
        requireNonNegative(nutrition.calories(), "칼로리");
        requireNonNegative(nutrition.carb(), "탄수화물");
        requireNonNegative(nutrition.protein(), "단백질");
        requireNonNegative(nutrition.fat(), "지방");
        requireNonNegative(nutrition.fiber(), "식이섬유");
        requireNonNegative(nutrition.sugar(), "당류");
        requireNonNegative(nutrition.sodium(), "나트륨");
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (value == null
                || value.signum() < 0
                || value.compareTo(MAX_NUTRITION_PER_GRAM) > 0) {
            throw generationFailure(field + " 영양값이 저장 범위를 벗어났습니다.");
        }
    }

    private void requirePositive(BigDecimal value, BigDecimal max, String field) {
        if (value == null || value.signum() <= 0 || value.compareTo(max) > 0) {
            throw generationFailure(field + " 값이 저장 범위를 벗어났습니다.");
        }
    }

    private void requireText(String value, int max, String field) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw generationFailure(field + " 값이 올바르지 않습니다.");
        }
    }

    private void optionalText(String value, int max, String field) {
        if (value != null && value.trim().length() > max) {
            throw generationFailure(field + " 값이 올바르지 않습니다.");
        }
    }

    private Ingredient resolveIngredient(String title) {
        String displayTitle = Ingredient.normalizeDisplayTitle(title);
        String normalizedTitle = Ingredient.normalizeTitle(title);
        ingredientRepository.insertIgnore(displayTitle, normalizedTitle);
        return ingredientRepository.findByNormalizedTitle(normalizedTitle)
                .orElseThrow(() -> generationFailure("대체 Ingredient를 저장하지 못했습니다."));
    }

    private NutritionSummary scaleNutrition(NutritionSummary nutrition) {
        return new NutritionSummary(
                scalePerGram(nutrition.calories()),
                scalePerGram(nutrition.carb()),
                scalePerGram(nutrition.protein()),
                scalePerGram(nutrition.fat()),
                scalePerGram(nutrition.fiber()),
                scalePerGram(nutrition.sugar()),
                scalePerGram(nutrition.sodium())
        );
    }

    private BigDecimal scalePerGram(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private RecipeIngredientResponse toIngredientResponse(RecipeIngredient ingredient) {
        return new RecipeIngredientResponse(
                ingredient.getIngredient().getId(),
                ingredient.getIngredient().getTitle(),
                ingredient.getAmount()
        );
    }

    private Recipe findOwnedRecipe(Long userId, Long recipeId) {
        return recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(RecipeNotFoundException::new);
    }

    private String requestKey(String userInput, List<Long> excludedSuggestionIds) {
        String excluded = excludedSuggestionIds.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String data = "v1|" + compact(userInput) + "|" + excluded;
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(data.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private String compact(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "");
    }

    private String trimNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RecipePersonalizationGenerationException generationFailure(String message) {
        return new RecipePersonalizationGenerationException(message);
    }

    private record LoadedContext(
            SubstitutionSuggestionContext context,
            Set<String> restrictedTerms,
            NutritionSummary originalContribution,
            IngredientAlternativeSuggestionListResponse cachedResponse,
            IngredientAlternativeSuggestionListResponse registeredResponse
    ) {
    }
}

