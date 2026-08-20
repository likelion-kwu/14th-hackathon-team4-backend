package com.glucobite.auth.service;

import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthConstraintViolationH2IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void recognizesNamedLoginIdConstraintFromH2Violation() {
        userRepository.saveAndFlush(new User("same-login", "hash", "첫 사용자"));

        DataIntegrityViolationException violation = catchThrowableOfType(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(
                        new User("same-login", "other-hash", "둘째 사용자")
                )
        );

        assertThat(violation).isNotNull();
        assertThat(new AuthConstraintViolationClassifier().isDuplicateLoginId(violation)).isTrue();
    }
}
