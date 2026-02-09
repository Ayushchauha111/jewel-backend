package com.example.jewell.controller;

import com.example.jewell.model.DaySession;
import com.example.jewell.service.DaySessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/day-session")
public class DaySessionController {
    @Autowired
    private DaySessionService daySessionService;

    @PostMapping("/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DaySession> openDay(@RequestParam(required = false) BigDecimal openingCash) {
        return ResponseEntity.ok(daySessionService.openDay(openingCash));
    }

    @PostMapping("/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DaySession> closeDay(
            @RequestParam(required = false) BigDecimal actualCashAtClose,
            @RequestParam(required = false) String mismatchReason) {
        return ResponseEntity.ok(daySessionService.closeDay(actualCashAtClose, mismatchReason));
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DaySession> getCurrentSession() {
        Optional<DaySession> session = daySessionService.getCurrentSession();
        return session.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/by-date")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DaySession> getSessionByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Optional<DaySession> session = daySessionService.getSessionByDate(date);
        return session.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
