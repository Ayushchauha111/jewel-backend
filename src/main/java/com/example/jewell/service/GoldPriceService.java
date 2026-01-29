package com.example.jewell.service;

import com.example.jewell.model.GoldPrice;
import com.example.jewell.repository.GoldPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GoldPriceService {
    @Autowired
    private GoldPriceRepository goldPriceRepository;

    public List<GoldPrice> getAllGoldPrices() {
        return goldPriceRepository.findAll();
    }

    public Optional<GoldPrice> getGoldPriceByDate(LocalDate date) {
        return goldPriceRepository.findByPriceDate(date);
    }

    public Optional<GoldPrice> getTodayGoldPrice() {
        return goldPriceRepository.findByPriceDate(LocalDate.now());
    }

    public Optional<GoldPrice> getLatestGoldPrice() {
        return goldPriceRepository.findFirstByOrderByPriceDateDesc();
    }

    public GoldPrice createOrUpdateGoldPrice(GoldPrice goldPrice) {
        Optional<GoldPrice> existing = goldPriceRepository.findByPriceDate(goldPrice.getPriceDate());
        if (existing.isPresent()) {
            GoldPrice existingPrice = existing.get();
            existingPrice.setPricePerGram(goldPrice.getPricePerGram());
            existingPrice.setPrice22Carat(goldPrice.getPrice22Carat());
            existingPrice.setPrice24Carat(goldPrice.getPrice24Carat());
            existingPrice.setNotes(goldPrice.getNotes());
            return goldPriceRepository.save(existingPrice);
        }
        return goldPriceRepository.save(goldPrice);
    }

    /**
     * Returns today's (or latest) gold price per gram for the given carat.
     * Uses price24Carat for 24K, price22Carat for 22K; otherwise derives from base price: base * (carat/24).
     */
    public Optional<BigDecimal> getPricePerGramForCarat(BigDecimal carat) {
        Optional<GoldPrice> gold = getTodayOrLatestGoldPrice();
        if (gold.isEmpty() || gold.get().getPricePerGram() == null || carat == null) {
            return Optional.empty();
        }
        GoldPrice gp = gold.get();
        BigDecimal rate;
        int caratInt = carat.intValue();
        if (caratInt == 24 && gp.getPrice24Carat() != null) {
            rate = gp.getPrice24Carat();
        } else if (caratInt == 22 && gp.getPrice22Carat() != null) {
            rate = gp.getPrice22Carat();
        } else {
            // Derive: base price (per gram, assumed 24K) * (carat/24)
            rate = gp.getPricePerGram().multiply(carat).divide(BigDecimal.valueOf(24), 2, RoundingMode.HALF_UP);
        }
        return Optional.of(rate);
    }

    /** Returns today's or latest gold price entity for rate date. */
    public Optional<GoldPrice> getTodayOrLatestGoldPrice() {
        return getTodayGoldPrice().isPresent() ? getTodayGoldPrice() : getLatestGoldPrice();
    }
}
