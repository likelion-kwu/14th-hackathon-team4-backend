package com.glucobite.auth.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AuthConstraintViolationClassifier {

    static final String LOGIN_ID_UNIQUE_CONSTRAINT = "uk_users_login_id";

    public boolean isDuplicateLoginId(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && isLoginIdConstraint(violation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    boolean isLoginIdConstraint(String constraintName) {
        if (constraintName == null || constraintName.isBlank()) {
            return false;
        }

        String normalized = constraintName
                .replace("`", "")
                .replace("\"", "")
                .toLowerCase(Locale.ROOT);
        int schemaSeparator = normalized.lastIndexOf('.');
        String unqualified = schemaSeparator >= 0
                ? normalized.substring(schemaSeparator + 1)
                : normalized;

        return unqualified.equals(LOGIN_ID_UNIQUE_CONSTRAINT)
                || unqualified.startsWith(LOGIN_ID_UNIQUE_CONSTRAINT + "_index_");
    }
}
