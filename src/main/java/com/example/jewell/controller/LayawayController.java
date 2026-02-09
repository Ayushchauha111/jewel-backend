package com.example.jewell.controller;

import com.example.jewell.model.Layaway;
import com.example.jewell.service.LayawayService;
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
@RequestMapping("/api/layaways")
public class LayawayController {
    @Autowired
    private LayawayService layawayService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Layaway> create(@RequestBody Layaway layaway) {
        return ResponseEntity.ok(layawayService.createLayaway(layaway));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Layaway> addPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(layawayService.addPayment(id, amount, notes));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Layaway> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(layawayService.cancel(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Layaway> getById(@PathVariable Long id) {
        return layawayService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Layaway>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(layawayService.getByCustomerId(customerId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Layaway>> getAll(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                Layaway.LayawayStatus s = Layaway.LayawayStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(layawayService.getByStatus(s));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(layawayService.getAll());
            }
        }
        return ResponseEntity.ok(layawayService.getAll());
    }
}
