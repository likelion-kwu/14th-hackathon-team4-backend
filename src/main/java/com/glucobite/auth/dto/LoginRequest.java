package com.glucobite.auth.dto;

import com.glucobite.common.validation.Utf8ByteLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디와 비밀번호 로그인 요청")
public record LoginRequest(
        @Schema(description = "로그인 아이디", example = "glucobite01")
        @NotBlank @Size(max = 100) String loginId,

        @Schema(description = "비밀번호, UTF-8 72바이트 이하", example = "password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Utf8ByteLength(max = 72) String password
) {
}
