package com.example.jewell.controller;

import com.example.jewell.model.Credit;
import com.example.jewell.model.CreditPaymentHistory;
import com.example.jewell.service.CreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/credits")
public class CreditController {
    @Autowired
    private CreditService creditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCredits(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Credit> creditPage = creditService.getAllCreditsPaginated(page, size);
        return ResponseEntity.ok(com.example.jewell.dto.PageResponse.of(creditPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Credit> getCreditById(@PathVariable Long id) {
        return creditService.getCreditById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Credit>> getCreditsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(creditService.getCreditsByCustomer(customerId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Credit>> getCreditsByStatus(@PathVariable Credit.CreditStatus status) {
        return ResponseEntity.ok(creditService.getCreditsByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Credit> createCredit(@RequestBody Credit credit) {
        return ResponseEntity.ok(creditService.createCredit(credit));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Credit> updateCreditPayment(
            @PathVariable Long id,
            @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(creditService.updateCreditPayment(id, request.getAmount()));
    }

    @GetMapping("/{id}/payment-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CreditPaymentHistory>> getPaymentHistory(@PathVariable Long id) {
        return ResponseEntity.ok(creditService.getPaymentHistory(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCredit(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            creditService.deleteCredit(id);
            response.put("success", true);
            response.put("message", "Udhari record deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting udhari record: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    public static class PaymentRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
