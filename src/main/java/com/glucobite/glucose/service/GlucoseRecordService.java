package com.glucobite.glucose.service;

import com.glucobite.glucose.dto.CreateGlucoseRecordRequest;
import com.glucobite.glucose.dto.GlucoseRecordListResponse;
import com.glucobite.glucose.dto.GlucoseRecordResponse;
import com.glucobite.glucose.entity.GlucoseRecord;
import com.glucobite.glucose.repository.GlucoseRecordRepository;
import com.glucobite.meal.entity.MealLog;
import com.glucobite.meal.exception.MealLogNotFoundException;
import com.glucobite.meal.repository.MealLogRepository;
import com.glucobite.tracking.service.TrackingDateRange;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GlucoseRecordService {

    private final GlucoseRecordRepository glucoseRecordRepository;
    private final MealLogRepository mealLogRepository;
    private final UserRepository userRepository;

    public GlucoseRecordService(
            GlucoseRecordRepository glucoseRecordRepository,
            MealLogRepository mealLogRepository,
            UserRepository userRepository
    ) {
        this.glucoseRecordRepository = glucoseRecordRepository;
        this.mealLogRepository = mealLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GlucoseRecordResponse create(Long userId, CreateGlucoseRecordRequest request) {
        User user = userRepository.getReferenceById(userId);
        MealLog mealLog = request.mealLogId() == null
                ? null
                : mealLogRepository.findByIdAndUserId(request.mealLogId(), userId)
                .orElseThrow(MealLogNotFoundException::new);
        return GlucoseRecordResponse.from(glucoseRecordRepository.save(new GlucoseRecord(
                user, mealLog, request.value(), request.context(), request.measuredAt()
        )));
    }

    @Transactional(readOnly = true)
    public GlucoseRecordListResponse get(Long userId, LocalDate from, LocalDate to) {
        TrackingDateRange range = TrackingDateRange.resolve(from, to);
        List<GlucoseRecordResponse> records = glucoseRecordRepository
                .findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(
                        userId, range.startInclusive(), range.endExclusive()
                ).stream()
                .map(GlucoseRecordResponse::from)
                .toList();
        return new GlucoseRecordListResponse(range.from(), range.to(), records);
    }
}
