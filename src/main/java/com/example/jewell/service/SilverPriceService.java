package com.example.jewell.service;

import com.example.jewell.model.SilverPrice;
import com.example.jewell.repository.SilverPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SilverPriceService {
    @Autowired
    private SilverPriceRepository silverPriceRepository;

    public List<SilverPrice> getAllSilverPrices() {
        return silverPriceRepository.findAll();
    }

    public Optional<SilverPrice> getSilverPriceByDate(LocalDate date) {
        return silverPriceRepository.findByPriceDate(date);
    }

    public Optional<SilverPrice> getTodaySilverPrice() {
        return silverPriceRepository.findByPriceDate(LocalDate.now());
    }

    public Optional<SilverPrice> getLatestSilverPrice() {
        return silverPriceRepository.findFirstByOrderByPriceDateDesc();
    }

    public SilverPrice createOrUpdateSilverPrice(SilverPrice silverPrice) {
        Optional<SilverPrice> existing = silverPriceRepository.findByPriceDate(silverPrice.getPriceDate());
        if (existing.isPresent()) {
            SilverPrice existingPrice = existing.get();
            existingPrice.setPricePerGram(silverPrice.getPricePerGram());
            existingPrice.setPricePerKg(silverPrice.getPricePerKg());
            existingPrice.setNotes(silverPrice.getNotes());
            return silverPriceRepository.save(existingPrice);
        }
        return silverPriceRepository.save(silverPrice);
    }
}
