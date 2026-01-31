package com.example.jewell.service;

import com.example.jewell.model.DailyRate;
import com.example.jewell.repository.DailyRateRepository;
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
public class DailyRateService {
    private static final List<Integer> GOLD_CARATS = List.of(10, 12, 14, 18, 20, 21, 22, 24);

    @Autowired
    private DailyRateRepository dailyRateRepository;

    public List<DailyRate> getAll() {
        return dailyRateRepository.findAll();
    }

    public Optional<DailyRate> getByDate(LocalDate date) {
        return dailyRateRepository.findByPriceDate(date);
    }

    public Optional<DailyRate> getTodayOrLatest() {
        return getByDate(LocalDate.now())
                .or(() -> dailyRateRepository.findFirstByOrderByPriceDateDesc());
    }

    public DailyRate save(DailyRate rate) {
        Optional<DailyRate> existing = dailyRateRepository.findByPriceDate(rate.getPriceDate());
        if (existing.isPresent()) {
            DailyRate e = existing.get();
            e.setGold10K(rate.getGold10K());
            e.setGold12K(rate.getGold12K());
            e.setGold14K(rate.getGold14K());
            e.setGold18K(rate.getGold18K());
            e.setGold20K(rate.getGold20K());
            e.setGold21K(rate.getGold21K());
            e.setGold22K(rate.getGold22K());
            e.setGold24K(rate.getGold24K());
            e.setSilverPerGram(rate.getSilverPerGram());
            e.setDiamondPerCarat(rate.getDiamondPerCarat());
            e.setNotes(rate.getNotes());
            return dailyRateRepository.save(e);
        }
        return dailyRateRepository.save(rate);
    }

    /**
     * Gold price per gram for the given carat. Uses stored value if present, else derives from 24K: rate24 * (carat/24).
     */
    public Optional<BigDecimal> getGoldRateForCarat(BigDecimal carat) {
        if (carat == null) return Optional.empty();
        Optional<DailyRate> opt = getTodayOrLatest();
        if (opt.isEmpty()) return Optional.empty();
        DailyRate r = opt.get();
        int c = carat.intValue();
        BigDecimal stored = getGoldCarat(r, c);
        if (stored != null && stored.compareTo(BigDecimal.ZERO) > 0) return Optional.of(stored);
        BigDecimal base24 = effectiveGold24K(r);
        if (base24 == null) return Optional.empty();
        return Optional.of(base24.multiply(carat).divide(BigDecimal.valueOf(24), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal effectiveGold24K(DailyRate r) {
        if (r.getGold24K() != null && r.getGold24K().compareTo(BigDecimal.ZERO) > 0) return r.getGold24K();
        if (r.getGold22K() != null && r.getGold22K().compareTo(BigDecimal.ZERO) > 0)
            return r.getGold22K().multiply(BigDecimal.valueOf(24)).divide(BigDecimal.valueOf(22), 2, RoundingMode.HALF_UP);
        if (r.getGold18K() != null && r.getGold18K().compareTo(BigDecimal.ZERO) > 0)
            return r.getGold18K().multiply(BigDecimal.valueOf(24)).divide(BigDecimal.valueOf(18), 2, RoundingMode.HALF_UP);
        if (r.getGold14K() != null && r.getGold14K().compareTo(BigDecimal.ZERO) > 0)
            return r.getGold14K().multiply(BigDecimal.valueOf(24)).divide(BigDecimal.valueOf(14), 2, RoundingMode.HALF_UP);
        if (r.getGold10K() != null && r.getGold10K().compareTo(BigDecimal.ZERO) > 0)
            return r.getGold10K().multiply(BigDecimal.valueOf(24)).divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        return null;
    }

    private BigDecimal getGoldCarat(DailyRate r, int carat) {
        return switch (carat) {
            case 10 -> r.getGold10K();
            case 12 -> r.getGold12K();
            case 14 -> r.getGold14K();
            case 18 -> r.getGold18K();
            case 20 -> r.getGold20K();
            case 21 -> r.getGold21K();
            case 22 -> r.getGold22K();
            case 24 -> r.getGold24K();
            default -> null;
        };
    }

    public Optional<BigDecimal> getSilverPerGram() {
        return getTodayOrLatest()
                .map(DailyRate::getSilverPerGram)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0);
    }

    public Optional<BigDecimal> getDiamondPerCarat() {
        return getTodayOrLatest()
                .map(DailyRate::getDiamondPerCarat)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0);
    }

    public List<Integer> getGoldCarats() {
        return GOLD_CARATS;
    }
}
