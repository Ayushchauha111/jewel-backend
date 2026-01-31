package com.example.jewell.controller;

import com.example.jewell.model.DailyRate;
import com.example.jewell.service.DailyRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rates")
public class DailyRateController {
    @Autowired
    private DailyRateService dailyRateService;

    @GetMapping
    public ResponseEntity<List<DailyRate>> getAll() {
        return ResponseEntity.ok(dailyRateService.getAll());
    }

    @GetMapping("/today")
    public ResponseEntity<DailyRate> getToday() {
        return dailyRateService.getTodayOrLatest()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public ResponseEntity<DailyRate> getLatest() {
        return dailyRateService.getTodayOrLatest()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<DailyRate> getByDate(@PathVariable LocalDate date) {
        return dailyRateService.getByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get rate for calculation: ?metal=GOLD&carat=22 | metal=SILVER | metal=DIAMOND
     */
    @GetMapping("/rate")
    public ResponseEntity<Map<String, Object>> getRate(
            @RequestParam(required = false) String metal,
            @RequestParam(required = false) Integer carat) {
        Map<String, Object> out = new java.util.HashMap<>();
        if (metal == null || metal.isBlank()) {
            out.put("error", "metal required (GOLD, SILVER, DIAMOND)");
            return ResponseEntity.badRequest().body(out);
        }
        switch (metal.toUpperCase()) {
            case "GOLD" -> {
                if (carat == null) {
                    out.put("error", "carat required for GOLD (10,12,14,18,20,21,22,24)");
                    return ResponseEntity.badRequest().body(out);
                }
                dailyRateService.getGoldRateForCarat(java.math.BigDecimal.valueOf(carat))
                        .ifPresent(rate -> out.put("ratePerGram", rate));
            }
            case "SILVER" -> dailyRateService.getSilverPerGram().ifPresent(rate -> out.put("ratePerGram", rate));
            case "DIAMOND" -> dailyRateService.getDiamondPerCarat().ifPresent(rate -> out.put("ratePerCarat", rate));
            default -> {
                out.put("error", "metal must be GOLD, SILVER, or DIAMOND");
                return ResponseEntity.badRequest().body(out);
            }
        }
        if (out.containsKey("error")) return ResponseEntity.badRequest().body(out);
        dailyRateService.getTodayOrLatest().ifPresent(r -> out.put("rateDate", r.getPriceDate().toString()));
        return ResponseEntity.ok(out);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DailyRate> save(@RequestBody DailyRate dailyRate) {
        return ResponseEntity.ok(dailyRateService.save(dailyRate));
    }

    @GetMapping("/carats")
    public ResponseEntity<List<Integer>> getGoldCarats() {
        return ResponseEntity.ok(dailyRateService.getGoldCarats());
    }
}
