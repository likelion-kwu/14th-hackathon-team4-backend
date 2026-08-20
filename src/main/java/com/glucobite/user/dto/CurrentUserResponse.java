package com.glucobite.user.dto;

import com.glucobite.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "현재 로그인 사용자 정보")
public record CurrentUserResponse(
        Long userId,
        String loginId,
        String nickname,
        LocalDateTime createdAt
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(), user.getLoginId(), user.getNickname(), user.getCreatedAt()
        );
    }
}
