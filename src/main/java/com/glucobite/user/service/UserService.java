package com.glucobite.user.service;

import com.glucobite.auth.exception.AuthenticatedUserNotFoundException;
import com.glucobite.user.dto.CurrentUserResponse;
import com.glucobite.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(CurrentUserResponse::from)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
    }
}
