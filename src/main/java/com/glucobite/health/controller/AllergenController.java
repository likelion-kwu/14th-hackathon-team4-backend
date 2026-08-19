package com.glucobite.health.controller;

import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.health.dto.AllergenResponse;
import com.glucobite.health.service.AllergenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/allergens")
@Tag(name = "Allergen", description = "가입용 알레르기 조회 API")
public class AllergenController {

    private final AllergenService allergenService;

    public AllergenController(AllergenService allergenService) {
        this.allergenService = allergenService;
    }

    @GetMapping
    @Operation(
            summary = "알레르기 목록 조회",
            description = "인증 없이 전체 알레르기 목록을 조회하거나 이름 일부로 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = AllergenResponse.class)
                    ))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어 길이 제한 초과",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public List<AllergenResponse> getAllergens(
            @Parameter(
                    description = "알레르기 이름 부분 검색어. 공백 또는 미입력 시 전체 목록을 반환합니다.",
                    example = "우유"
            )
            @RequestParam(required = false)
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
            String query
    ) {
        return allergenService.findAll(query);
    }
}
