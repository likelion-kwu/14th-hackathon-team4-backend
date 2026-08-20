package com.glucobite.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PastOrPresentKstValidatorTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesAgainstKstInsteadOfJvmDefaultTimezone() {
        LocalDateTime kstNow = LocalDateTime.now(KST);

        assertThat(validator.validate(new TestValue(kstNow.minusSeconds(1)))).isEmpty();
        assertThat(validator.validate(new TestValue(kstNow.plusMinutes(1)))).isNotEmpty();
    }

    private record TestValue(@PastOrPresentKst LocalDateTime value) {
    }
}
