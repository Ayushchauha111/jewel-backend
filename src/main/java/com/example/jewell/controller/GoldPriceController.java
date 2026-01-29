package com.example.jewell.controller;

import com.example.jewell.model.GoldPrice;
import com.example.jewell.service.GoldPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/gold-price")
public class GoldPriceController {
    @Autowired
    private GoldPriceService goldPriceService;

    @GetMapping
    public ResponseEntity<List<GoldPrice>> getAllGoldPrices() {
        return ResponseEntity.ok(goldPriceService.getAllGoldPrices());
    }

    @GetMapping("/today")
    public ResponseEntity<GoldPrice> getTodayGoldPrice() {
        // Try to get today's price, if not found, return the latest price
        return goldPriceService.getTodayGoldPrice()
                .map(ResponseEntity::ok)
                .or(() -> goldPriceService.getLatestGoldPrice().map(ResponseEntity::ok))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public ResponseEntity<GoldPrice> getLatestGoldPrice() {
        return goldPriceService.getLatestGoldPrice()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<GoldPrice> getGoldPriceByDate(@PathVariable LocalDate date) {
        return goldPriceService.getGoldPriceByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GoldPrice> createOrUpdateGoldPrice(@RequestBody GoldPrice goldPrice) {
        return ResponseEntity.ok(goldPriceService.createOrUpdateGoldPrice(goldPrice));
    }
}
