package com.example.jewell.controller;

import com.example.jewell.model.GiftVoucher;
import com.example.jewell.service.GiftVoucherService;
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
@RequestMapping("/api/gift-vouchers")
public class GiftVoucherController {
    @Autowired
    private GiftVoucherService giftVoucherService;

    @PostMapping("/sell")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GiftVoucher> sellVoucher(@RequestParam BigDecimal amount) {
        return ResponseEntity.ok(giftVoucherService.sellVoucher(amount));
    }

    @GetMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> validate(@RequestParam String code) {
        Optional<BigDecimal> amt = giftVoucherService.validateAndGetAmount(code);
        return ResponseEntity.ok(Map.of(
                "valid", amt.isPresent(),
                "amount", amt.orElse(BigDecimal.ZERO)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GiftVoucher>> getAll() {
        return ResponseEntity.ok(giftVoucherService.findAll());
    }
}
