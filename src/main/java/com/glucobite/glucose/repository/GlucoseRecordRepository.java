package com.glucobite.glucose.repository;

import com.glucobite.glucose.entity.GlucoseRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GlucoseRecordRepository extends JpaRepository<GlucoseRecord, Long> {

    @EntityGraph(attributePaths = "mealLog")
    List<GlucoseRecord> findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(
            Long userId,
            LocalDateTime from,
            LocalDateTime toExclusive
    );
}
