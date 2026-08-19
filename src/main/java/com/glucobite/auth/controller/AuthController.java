package com.glucobite.auth.controller;

import com.glucobite.auth.dto.LoginRequest;
import com.glucobite.auth.dto.SignupRequest;
import com.glucobite.auth.dto.TokenResponse;
import com.glucobite.auth.service.AuthService;
import com.glucobite.common.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원가입 및 로그인 API")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "통합 회원가입",
            description = "계정, 건강 프로필, 알레르기 정보를 하나의 트랜잭션으로 저장하고 액세스 토큰을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 validation 실패 또는 존재하지 않는 알레르기",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "로그인 아이디 중복",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public TokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "아이디와 비밀번호를 검증하고 액세스 토큰을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 validation 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "아이디 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
