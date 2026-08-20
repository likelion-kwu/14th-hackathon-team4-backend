package com.glucobite.auth.service;

import com.glucobite.user.entity.User;

public record RotatedRefreshToken(
        User user,
        String rawToken
) {
}
