package com.glucobite.auth.service;

import com.glucobite.auth.dto.LoginRequest;
import com.glucobite.auth.dto.SignupProfileRequest;
import com.glucobite.auth.dto.SignupRequest;
import com.glucobite.auth.dto.TokenResponse;
import com.glucobite.auth.exception.DuplicateLoginIdException;
import com.glucobite.auth.exception.AccountPersistenceException;
import com.glucobite.auth.exception.InvalidAllergenException;
import com.glucobite.auth.exception.InvalidCredentialsException;
import com.glucobite.common.security.JwtTokenService;
import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.repository.AllergenRepository;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final AllergenRepository allergenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthConstraintViolationClassifier constraintViolationClassifier;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            HealthProfileRepository healthProfileRepository,
            AllergenRepository allergenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuthConstraintViolationClassifier constraintViolationClassifier
    ) {
        this.userRepository = userRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.allergenRepository = allergenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.constraintViolationClassifier = constraintViolationClassifier;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing-protection");
    }

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new DuplicateLoginIdException();
        }

        User user = saveUser(request);
        SignupProfileRequest profileRequest = request.profile();
        List<Allergen> allergens = findAllergens(profileRequest.allergenIds());
        HealthProfile healthProfile = new HealthProfile(
                user,
                profileRequest.birthDate(),
                profileRequest.height(),
                profileRequest.weight(),
                profileRequest.sex(),
                profileRequest.healthGoal(),
                null,
                profileRequest.glucoseDeviceConnected(),
                profileRequest.dailyCarbsTarget(),
                profileRequest.vegetarianType(),
                profileRequest.dietaryRestrictionNote(),
                allergens
        );
        healthProfileRepository.save(healthProfile);

        return TokenResponse.from(jwtTokenService.issue(user.getId()));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<User> foundUser = userRepository.findByLoginId(request.loginId());
        String passwordHash = foundUser
                .map(User::getPassword)
                .orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (foundUser.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return TokenResponse.from(jwtTokenService.issue(foundUser.orElseThrow().getId()));
    }

    private User saveUser(SignupRequest request) {
        try {
            return userRepository.saveAndFlush(new User(
                    request.loginId(),
                    passwordEncoder.encode(request.password()),
                    request.nickname()
            ));
        } catch (DataIntegrityViolationException exception) {
            if (constraintViolationClassifier.isDuplicateLoginId(exception)) {
                throw new DuplicateLoginIdException(exception);
            }
            throw new AccountPersistenceException(exception);
        }
    }

    private List<Allergen> findAllergens(Set<Long> allergenIds) {
        List<Allergen> allergens = allergenRepository.findAllById(allergenIds);
        if (allergens.size() != allergenIds.size()) {
            throw new InvalidAllergenException();
        }
        return allergens;
    }
}
