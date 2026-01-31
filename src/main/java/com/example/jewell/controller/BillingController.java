package com.example.jewell.controller;

import com.example.jewell.dto.PageResponse;
import com.example.jewell.model.Billing;
import com.example.jewell.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/billing")
public class BillingController {
    @Autowired
    private BillingService billingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBills(@RequestParam(required = false) String search,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Page<Billing> billingPage;
        if (search != null && !search.trim().isEmpty()) {
            billingPage = billingService.searchBillsPaginated(search, page, size);
        } else {
            billingPage = billingService.getAllBillsPaginated(page, size);
        }
        return ResponseEntity.ok(PageResponse.of(billingPage));
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Billing>> searchBills(@RequestParam String query) {
        return ResponseEntity.ok(billingService.searchBills(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Billing> getBillById(@PathVariable Long id) {
        return billingService.getBillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{billNumber}")
    public ResponseEntity<Billing> getBillByNumber(@PathVariable String billNumber) {
        return billingService.getBillByBillNumber(billNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Billing> createBill(@RequestBody Billing billing) {
        return ResponseEntity.ok(billingService.createBill(billing));
    }

    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendBillEmail(@PathVariable Long id,
                                              @RequestParam(required = false, defaultValue = "NORMAL") String receiptType) {
        billingService.sendBillViaEmail(id, receiptType);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/send-whatsapp")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendBillWhatsApp(@PathVariable Long id,
                                                @RequestParam(required = false, defaultValue = "NORMAL") String receiptType) {
        billingService.sendBillViaWhatsApp(id, receiptType);
        return ResponseEntity.ok().build();
    }
}
