package com.glucobite.auth.service;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthConstraintViolationClassifierTest {

    private final AuthConstraintViolationClassifier classifier =
            new AuthConstraintViolationClassifier();

    @Test
    void identifiesLoginIdConstraintThroughCauseChain() {
        ConstraintViolationException hibernateException = mock(ConstraintViolationException.class);
        when(hibernateException.getConstraintName()).thenReturn("uk_users_login_id");

        assertThat(classifier.isDuplicateLoginId(
                new DataIntegrityViolationException("save failed", hibernateException)
        )).isTrue();
    }

    @Test
    void acceptsQualifiedAndQuotedMysqlConstraintNames() {
        assertThat(classifier.isLoginIdConstraint("glucobite.`uk_users_login_id`")).isTrue();
        assertThat(classifier.isLoginIdConstraint("PUBLIC.\"UK_USERS_LOGIN_ID_INDEX_4\"")).isTrue();
    }

    @Test
    void rejectsOtherOrUnknownConstraints() {
        ConstraintViolationException otherConstraint = mock(ConstraintViolationException.class);
        when(otherConstraint.getConstraintName()).thenReturn("uk_health_profiles_user_id");
        ConstraintViolationException unnamedConstraint = mock(ConstraintViolationException.class);
        when(unnamedConstraint.getConstraintName()).thenReturn(null);

        assertThat(classifier.isDuplicateLoginId(otherConstraint)).isFalse();
        assertThat(classifier.isDuplicateLoginId(unnamedConstraint)).isFalse();
        assertThat(classifier.isDuplicateLoginId(
                new DataIntegrityViolationException("unknown")
        )).isFalse();
    }
}
