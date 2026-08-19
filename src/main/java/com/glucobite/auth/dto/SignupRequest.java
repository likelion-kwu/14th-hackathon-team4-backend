package com.glucobite.auth.dto;

import com.glucobite.common.validation.Utf8ByteLength;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "건강 프로필을 포함한 통합 회원가입 요청")
public record SignupRequest(
        @Schema(description = "로그인 아이디", example = "glucobite01")
        @NotBlank @Size(min = 4, max = 100) String loginId,

        @Schema(description = "비밀번호, 8자 이상 UTF-8 72바이트 이하", example = "password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 8) @Utf8ByteLength(max = 72) String password,

        @Schema(description = "서비스에서 사용할 닉네임", example = "건강한끼")
        @NotBlank @Size(max = 50) String nickname,

        @Schema(description = "가입 시 저장할 건강 프로필")
        @NotNull @Valid SignupProfileRequest profile
) {
}
