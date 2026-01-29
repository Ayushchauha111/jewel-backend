package com.example.jewell.service;
import java.time.LocalDate;
import java.util.List;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.jewell.model.Coupon;
import com.example.jewell.repository.CouponRepository;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    // Create a new coupon
    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    // Validate a coupon
    public boolean validateCoupon(String code) {
        Optional<Coupon> coupon = couponRepository.findByCode(code);
        return coupon.isPresent() && !coupon.get().getExpiryDate().isBefore(LocalDate.now());
    }

    // Get all coupons
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    // Update a coupon
    public Coupon updateCoupon(Long id, Coupon couponDetails) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found with id: " + id));
        
        coupon.setCode(couponDetails.getCode());
        coupon.setDiscount(couponDetails.getDiscount());
        coupon.setExpiryDate(couponDetails.getExpiryDate());
        
        return couponRepository.save(coupon);
    }

    // Get coupon by ID
    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }

    // Delete a coupon by ID
    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }
}