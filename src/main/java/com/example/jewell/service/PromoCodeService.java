package com.example.jewell.service;

import com.example.jewell.model.PromoCode;
import com.example.jewell.repository.PromoCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PromoCodeService {
    @Autowired
    private PromoCodeRepository promoCodeRepository;

    /**
     * Validate promo code and compute discount for given subtotal (before discount).
     * Returns discount amount to subtract, or empty if invalid.
     */
    public Optional<BigDecimal> validateAndGetDiscount(String code, BigDecimal subtotal) {
        if (code == null || code.trim().isEmpty() || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        Optional<PromoCode> opt = promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim());
        if (opt.isEmpty()) return Optional.empty();
        PromoCode p = opt.get();
        LocalDate today = LocalDate.now();
        if (p.getValidFrom() != null && today.isBefore(p.getValidFrom())) return Optional.empty();
        if (p.getValidUntil() != null && today.isAfter(p.getValidUntil())) return Optional.empty();
        if (p.getMinPurchaseAmount() != null && subtotal.compareTo(p.getMinPurchaseAmount()) < 0) return Optional.empty();
        if (p.getMaxUses() != null && p.getUsedCount() != null && p.getUsedCount() >= p.getMaxUses()) return Optional.empty();

        BigDecimal discount = BigDecimal.ZERO;
        if (p.getDiscountType() == PromoCode.DiscountType.PERCENT) {
            discount = subtotal.multiply(p.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
        } else {
            discount = p.getDiscountValue();
        }
        if (p.getMaxDiscountAmount() != null && discount.compareTo(p.getMaxDiscountAmount()) > 0) {
            discount = p.getMaxDiscountAmount();
        }
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        return Optional.of(discount);
    }

    /**
     * Apply promo: validate, return discount, and increment used count (call after bill is saved).
     */
    public Optional<BigDecimal> applyAndIncrementUsage(String code, BigDecimal subtotal) {
        Optional<BigDecimal> discount = validateAndGetDiscount(code, subtotal);
        if (discount.isEmpty()) return Optional.empty();
        PromoCode p = promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim()).orElse(null);
        if (p != null) {
            p.setUsedCount((p.getUsedCount() != null ? p.getUsedCount() : 0) + 1);
            promoCodeRepository.save(p);
        }
        return discount;
    }

    public List<PromoCode> findAll() {
        return promoCodeRepository.findAll();
    }

    public Optional<PromoCode> findById(Long id) {
        return promoCodeRepository.findById(id);
    }

    public Optional<PromoCode> findByCode(String code) {
        return promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(code);
    }

    public PromoCode save(PromoCode promoCode) {
        return promoCodeRepository.save(promoCode);
    }

    public void deleteById(Long id) {
        promoCodeRepository.deleteById(id);
    }
}
