package com.glucobite.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "건강 프로필을 포함한 통합 회원가입 요청")
public record SignupRequest(
        @Schema(description = "로그인 아이디", example = "glucobite01")
        @NotBlank @Size(min = 4, max = 100) String loginId,

        @Schema(description = "비밀번호, 4자리 숫자", example = "1234", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Pattern(regexp = "[0-9]{4}", message = "비밀번호는 4자리 숫자여야 합니다.") String password,

        @Schema(description = "서비스에서 사용할 닉네임", example = "건강한끼")
        @NotBlank @Size(max = 50) String nickname,

        @Schema(description = "가입 시 저장할 건강 프로필")
        @NotNull @Valid SignupProfileRequest profile
) {
}
