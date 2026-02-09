package com.example.jewell.service;

import com.example.jewell.model.Billing;
import com.example.jewell.model.GiftVoucher;
import com.example.jewell.repository.GiftVoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GiftVoucherService {
    @Autowired
    private GiftVoucherRepository giftVoucherRepository;

    /**
     * Sell a new voucher: create with unique code and amount, status ISSUED.
     */
    public GiftVoucher sellVoucher(BigDecimal amount) {
        GiftVoucher v = new GiftVoucher();
        v.setCode("GV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        v.setAmount(amount);
        v.setStatus(GiftVoucher.VoucherStatus.ISSUED);
        v.setSoldAt(LocalDateTime.now());
        return giftVoucherRepository.save(v);
    }

    /**
     * Validate voucher and return discount amount if valid and not yet redeemed.
     */
    public Optional<BigDecimal> validateAndGetAmount(String code) {
        if (code == null || code.trim().isEmpty()) return Optional.empty();
        return giftVoucherRepository.findByCodeIgnoreCaseAndStatus(code.trim(), GiftVoucher.VoucherStatus.ISSUED)
                .map(GiftVoucher::getAmount);
    }

    /**
     * Mark voucher as redeemed against billing (call after bill is saved).
     */
    public void redeemVoucher(String code, Billing billing) {
        if (code == null || code.trim().isEmpty() || billing == null) return;
        giftVoucherRepository.findByCodeIgnoreCaseAndStatus(code.trim(), GiftVoucher.VoucherStatus.ISSUED)
                .ifPresent(v -> {
                    v.setStatus(GiftVoucher.VoucherStatus.REDEEMED);
                    v.setRedeemedAt(LocalDateTime.now());
                    v.setRedeemedAgainstBilling(billing);
                    giftVoucherRepository.save(v);
                });
    }

    public List<GiftVoucher> findAll() {
        return giftVoucherRepository.findAll();
    }

    public Optional<GiftVoucher> findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return Optional.empty();
        return giftVoucherRepository.findByCodeIgnoreCaseAndStatus(code.trim(), GiftVoucher.VoucherStatus.ISSUED);
    }
}
