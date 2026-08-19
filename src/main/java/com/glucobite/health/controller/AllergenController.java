package com.glucobite.health.controller;

import com.glucobite.health.dto.AllergenResponse;
import com.glucobite.health.service.AllergenService;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/allergens")
public class AllergenController {

    private final AllergenService allergenService;

    public AllergenController(AllergenService allergenService) {
        this.allergenService = allergenService;
    }

    @GetMapping
    public List<AllergenResponse> getAllergens(
            @RequestParam(required = false)
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
            String query
    ) {
        return allergenService.findAll(query);
    }
}
