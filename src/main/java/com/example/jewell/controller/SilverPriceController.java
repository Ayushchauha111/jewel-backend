package com.example.jewell.controller;

import com.example.jewell.model.SilverPrice;
import com.example.jewell.service.SilverPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/silver-price")
public class SilverPriceController {
    @Autowired
    private SilverPriceService silverPriceService;

    @GetMapping
    public ResponseEntity<List<SilverPrice>> getAllSilverPrices() {
        return ResponseEntity.ok(silverPriceService.getAllSilverPrices());
    }

    @GetMapping("/today")
    public ResponseEntity<SilverPrice> getTodaySilverPrice() {
        return silverPriceService.getTodaySilverPrice()
                .map(ResponseEntity::ok)
                .or(() -> silverPriceService.getLatestSilverPrice().map(ResponseEntity::ok))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public ResponseEntity<SilverPrice> getLatestSilverPrice() {
        return silverPriceService.getLatestSilverPrice()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<SilverPrice> getSilverPriceByDate(@PathVariable LocalDate date) {
        return silverPriceService.getSilverPriceByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SilverPrice> createOrUpdateSilverPrice(@RequestBody SilverPrice silverPrice) {
        return ResponseEntity.ok(silverPriceService.createOrUpdateSilverPrice(silverPrice));
    }
}
