package com.glucobite.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Utf8ByteLengthValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAsciiAndKoreanValuesAtSeventyTwoByteBoundary() {
        assertThat(validator.validate(new PasswordValue("a".repeat(72)))).isEmpty();
        assertThat(validator.validate(new PasswordValue("가".repeat(24)))).isEmpty();
    }

    @Test
    void rejectsAsciiAndKoreanValuesOverSeventyTwoBytes() {
        assertThat(validator.validate(new PasswordValue("a".repeat(73)))).hasSize(1);
        assertThat(validator.validate(new PasswordValue("가".repeat(25)))).hasSize(1);
    }

    @Test
    void leavesNullHandlingToOtherConstraints() {
        assertThat(validator.validate(new PasswordValue(null))).isEmpty();
    }

    private record PasswordValue(
            @Utf8ByteLength(max = 72) String value
    ) {
    }
}
