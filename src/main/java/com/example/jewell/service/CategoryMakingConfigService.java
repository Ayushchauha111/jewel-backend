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
        return repository.findAllByOrderByCategoryAsc();
    }

    public Optional<CategoryMakingConfig> findByCategory(String category) {
        if (category == null || category.isBlank()) return Optional.empty();
        return repository.findByCategoryIgnoreCase(category.trim());
    }

    /**
     * Returns making charges per gram for the given category, or empty if not configured.
     */
    public Optional<BigDecimal> getMakingChargesPerGramForCategory(String category) {
        return findByCategory(category)
                .map(CategoryMakingConfig::getMakingChargesPerGram)
                .filter(mc -> mc != null && mc.compareTo(BigDecimal.ZERO) >= 0);
    }

    @Transactional
    public CategoryMakingConfig save(CategoryMakingConfig config) {
        if (config.getCategory() != null) {
            config.setCategory(config.getCategory().trim());
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
