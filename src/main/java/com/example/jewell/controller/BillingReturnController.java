package com.example.jewell.controller;

import com.example.jewell.dto.PageResponse;
import com.example.jewell.model.BillingReturn;
import com.example.jewell.service.BillingReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/billing-returns")
public class BillingReturnController {
    @Autowired
    private BillingReturnService billingReturnService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BillingReturn> createReturn(@RequestBody BillingReturn returnRequest) {
        return ResponseEntity.ok(billingReturnService.createReturn(returnRequest));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BillingReturn> getById(@PathVariable Long id) {
        return billingReturnService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-billing/{billingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BillingReturn>> getByBillingId(@PathVariable Long billingId) {
        return ResponseEntity.ok(billingReturnService.getByOriginalBillingId(billingId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BillingReturn> returnPage = billingReturnService.getAllPaginated(page, size);
        return ResponseEntity.ok(PageResponse.of(returnPage));
    }
}
