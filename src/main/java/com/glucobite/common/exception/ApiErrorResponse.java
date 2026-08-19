package com.glucobite.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "공통 API 오류 응답")
public record ApiErrorResponse(
        @Schema(description = "애플리케이션 오류 코드", example = "VALIDATION_ERROR")
        String code,

        @Schema(description = "사용자에게 표시할 오류 메시지", example = "요청 값을 확인해 주세요.")
        String message,

        @Schema(description = "필드별 validation 오류, 해당 사항이 없으면 빈 객체")
        Map<String, String> fieldErrors
) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, Map.of());
    }
}
