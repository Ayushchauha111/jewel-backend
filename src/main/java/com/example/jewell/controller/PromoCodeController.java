package com.example.jewell.controller;

import com.example.jewell.model.PromoCode;
import com.example.jewell.service.PromoCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/promo-codes")
public class PromoCodeController {
    @Autowired
    private PromoCodeService promoCodeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromoCode>> getAll() {
        return ResponseEntity.ok(promoCodeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> getById(@PathVariable Long id) {
        return promoCodeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Validate promo code and get discount amount for given subtotal (e.g. for billing preview).
     */
    @GetMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {
        Optional<BigDecimal> discount = promoCodeService.validateAndGetDiscount(code, subtotal);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("valid", discount.isPresent());
        result.put("discountAmount", discount.orElse(BigDecimal.ZERO));
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> create(@RequestBody PromoCode promoCode) {
        return ResponseEntity.ok(promoCodeService.save(promoCode));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> update(@PathVariable Long id, @RequestBody PromoCode promoCode) {
        if (!promoCodeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        promoCode.setId(id);
        return ResponseEntity.ok(promoCodeService.save(promoCode));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!promoCodeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        promoCodeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
