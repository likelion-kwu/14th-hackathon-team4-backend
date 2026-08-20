package com.glucobite.health.controller;

import com.glucobite.health.entity.Allergen;
import com.glucobite.health.repository.AllergenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AllergenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AllergenRepository allergenRepository;

    @Test
    void returnsAllSeedAllergensWithoutAuthenticationInIdOrder() throws Exception {
        mockMvc.perform(get("/api/allergens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(19)))
                .andExpect(jsonPath("$[0].allergenId").isNumber())
                .andExpect(jsonPath("$[0].name").value("난류"))
                .andExpect(jsonPath("$[18].name").value("잣"))
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void treatsMissingOrBlankQueryAsFullList() throws Exception {
        mockMvc.perform(get("/api/allergens").param("query", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(19)));
    }

    @Test
    void trimsQueryAndReturnsPartialMatches() throws Exception {
        mockMvc.perform(get("/api/allergens").param("query", "  우유  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("우유"));
    }

    @Test
    void searchesEnglishNamesIgnoringCase() throws Exception {
        allergenRepository.saveAndFlush(new Allergen("Tree Nut"));

        mockMvc.perform(get("/api/allergens").param("query", "tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Tree Nut"));
    }

    @Test
    void returnsEmptyListWhenNoAllergenMatches() throws Exception {
        mockMvc.perform(get("/api/allergens").param("query", "없는 알레르기"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsQueryLongerThanOneHundredCharacters() throws Exception {
        mockMvc.perform(get("/api/allergens").param("query", "가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("요청 값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors.query")
                        .value("검색어는 100자 이하여야 합니다."));
    }

    @Test
    void keepsOtherAllergenMethodsProtected() throws Exception {
        mockMvc.perform(post("/api/allergens"))
                .andExpect(status().isUnauthorized());
    }
}
