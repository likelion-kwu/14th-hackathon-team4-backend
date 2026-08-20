package com.glucobite.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class PastOrPresentKstValidator
        implements ConstraintValidator<PastOrPresentKst, LocalDateTime> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        return value == null || !value.isAfter(LocalDateTime.now(KST));
    }
}
