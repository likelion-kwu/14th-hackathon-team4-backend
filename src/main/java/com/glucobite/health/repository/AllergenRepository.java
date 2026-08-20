package com.glucobite.health.repository;

import com.glucobite.health.entity.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllergenRepository extends JpaRepository<Allergen, Long> {

    Optional<Allergen> findByName(String name);

    List<Allergen> findAllByOrderByIdAsc();

    List<Allergen> findByNameContainingIgnoreCaseOrderByIdAsc(String name);
}
