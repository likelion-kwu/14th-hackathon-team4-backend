package com.glucobite.health.service;

import com.glucobite.health.dto.AllergenResponse;
import com.glucobite.health.entity.Allergen;
import com.glucobite.health.repository.AllergenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AllergenService {

    private final AllergenRepository allergenRepository;

    public AllergenService(AllergenRepository allergenRepository) {
        this.allergenRepository = allergenRepository;
    }

    @Transactional(readOnly = true)
    public List<AllergenResponse> findAll(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        List<Allergen> allergens = normalizedQuery.isEmpty()
                ? allergenRepository.findAllByOrderByIdAsc()
                : allergenRepository.findByNameContainingIgnoreCaseOrderByIdAsc(normalizedQuery);

        return allergens.stream()
                .map(AllergenResponse::from)
                .toList();
    }
}
