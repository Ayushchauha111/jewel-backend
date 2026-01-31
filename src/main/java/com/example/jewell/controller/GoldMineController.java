package com.example.jewell.controller;

import com.example.jewell.model.GoldMinePlan;
import com.example.jewell.service.GoldMineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/gold-mine")
public class GoldMineController {

    @Autowired
    private GoldMineService goldMineService;

    /** Public: calculator defaults (min/max amount, constants). */
    @GetMapping("/calculator")
    public ResponseEntity<Map<String, Object>> getCalculatorDefaults() {
        return ResponseEntity.ok(goldMineService.getCalculatorDefaults());
    }

    /** Enroll: create a new 10+1 plan for a customer. Public for enrollment flow; can require auth later. */
    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(@RequestBody Map<String, Object> body) {
        Object customerIdObj = body.get("customerId");
        Object monthlyAmountObj = body.get("monthlyAmount");
        if (customerIdObj == null || monthlyAmountObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "customerId and monthlyAmount are required"));
        }
        Long customerId = customerIdObj instanceof Number ? ((Number) customerIdObj).longValue() : Long.parseLong(customerIdObj.toString());
        BigDecimal monthlyAmount = monthlyAmountObj instanceof BigDecimal ? (BigDecimal) monthlyAmountObj : new BigDecimal(monthlyAmountObj.toString());
        try {
            GoldMinePlan plan = goldMineService.createPlan(customerId, monthlyAmount);
            return ResponseEntity.ok(plan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (com.example.jewell.exception.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Get plan by id (with installments). */
    @GetMapping("/plans/{id}")
    public ResponseEntity<GoldMinePlan> getPlan(@PathVariable Long id) {
        return goldMineService.getPlanWithInstallments(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** List plans: by customerId (my plans) or all with pagination (admin). */
    @GetMapping("/plans")
    public ResponseEntity<?> listPlans(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) GoldMinePlan.PlanStatus status) {
        if (customerId != null) {
            List<GoldMinePlan> list = goldMineService.getPlansByCustomer(customerId);
            return ResponseEntity.ok(list);
        }
        return ResponseEntity.ok(com.example.jewell.dto.PageResponse.of(goldMineService.getAllPlans(page, size, status)));
    }

    /** Record payment for an installment (1-10). Admin or staff. */
    @PostMapping("/plans/{planId}/installments/{installmentNumber}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recordPayment(
            @PathVariable Long planId,
            @PathVariable int installmentNumber,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("amount") != null
                ? new BigDecimal(body.get("amount").toString())
                : null;
        String paymentRef = body.get("paymentReference") != null ? body.get("paymentReference").toString() : null;
        try {
            GoldMinePlan plan = goldMineService.recordPayment(planId, installmentNumber, amount, paymentRef);
            return ResponseEntity.ok(plan);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (com.example.jewell.exception.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Redeem 11th installment (mark plan completed). Admin or when 10 paid. */
    @PostMapping("/plans/{planId}/redeem")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> redeem(@PathVariable Long planId) {
        try {
            GoldMinePlan plan = goldMineService.redeem(planId);
            return ResponseEntity.ok(plan);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (com.example.jewell.exception.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Cancel an active plan. Admin. */
    @PostMapping("/plans/{planId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancel(@PathVariable Long planId) {
        try {
            GoldMinePlan plan = goldMineService.cancel(planId);
            return ResponseEntity.ok(plan);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (com.example.jewell.exception.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
