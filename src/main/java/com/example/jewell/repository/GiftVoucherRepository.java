package com.example.jewell.repository;

import com.example.jewell.model.GiftVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GiftVoucherRepository extends JpaRepository<GiftVoucher, Long> {
    Optional<GiftVoucher> findByCodeIgnoreCaseAndStatus(String code, GiftVoucher.VoucherStatus status);
}
