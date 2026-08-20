package com.glucobite.meal.repository;

import com.glucobite.meal.entity.MealLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    @EntityGraph(attributePaths = "recipe")
    List<MealLog> findByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtDescIdDesc(
            Long userId,
            LocalDateTime from,
            LocalDateTime toExclusive
    );

    Optional<MealLog> findByIdAndUserId(Long id, Long userId);
}
