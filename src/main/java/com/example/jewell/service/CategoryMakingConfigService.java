package com.example.jewell.service;

import com.example.jewell.model.CategoryMakingConfig;
import com.example.jewell.repository.CategoryMakingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryMakingConfigService {

    @Autowired
    private CategoryMakingConfigRepository repository;

    public List<CategoryMakingConfig> findAll() {
        return repository.findAllByOrderByCategoryAscMaterialAsc();
    }

    /**
     * Returns making charges per gram for the given category and material.
     * Lookup order: category+material, then category only (material null).
     */
    public Optional<BigDecimal> getMakingChargesPerGramForCategoryAndMaterial(String category, String material) {
        if (category == null || category.isBlank()) return Optional.empty();
        String cat = category.trim();
        if (material != null && !material.isBlank()) {
            Optional<BigDecimal> byMaterial = repository.findByCategoryIgnoreCaseAndMaterialIgnoreCase(cat, material.trim())
                    .map(CategoryMakingConfig::getMakingChargesPerGram)
                    .filter(mc -> mc != null && mc.compareTo(BigDecimal.ZERO) >= 0);
            if (byMaterial.isPresent()) return byMaterial;
        }
        return repository.findByCategoryIgnoreCaseAndMaterialIsNull(cat)
                .map(CategoryMakingConfig::getMakingChargesPerGram)
                .filter(mc -> mc != null && mc.compareTo(BigDecimal.ZERO) >= 0);
    }

    /** Backward compatibility: category only (no material). */
    public Optional<BigDecimal> getMakingChargesPerGramForCategory(String category) {
        return getMakingChargesPerGramForCategoryAndMaterial(category, null);
    }

    @Transactional
    public CategoryMakingConfig save(CategoryMakingConfig config) {
        if (config.getCategory() != null) {
            config.setCategory(config.getCategory().trim());
        }
        if (config.getMaterial() != null && config.getMaterial().isBlank()) {
            config.setMaterial(null);
        } else if (config.getMaterial() != null) {
            config.setMaterial(config.getMaterial().trim());
        }
        return repository.save(config);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public Optional<CategoryMakingConfig> findById(Long id) {
        return repository.findById(id);
    }
}
