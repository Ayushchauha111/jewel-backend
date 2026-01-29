package com.example.jewell.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.model.Coupon;
import com.example.jewell.service.CouponService;

@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CouponController {

    @Autowired
    private CouponService couponService;

    // Create a new coupon (admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Coupon>> createCoupon(@RequestBody Coupon coupon) {
        try {
            Coupon createdCoupon = couponService.createCoupon(coupon);
            return ResponseEntity.ok(new ApiResponseDTO<Coupon>(true, "coupon created successfully", createdCoupon));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<Coupon>(false, e.getMessage(), null));
        }
        
    }

    // Validate a coupon
    @GetMapping("/validate")
    public ResponseEntity<ApiResponseDTO<Object>> validateCoupon(@RequestParam String code) {
       
        try {
            boolean isValid = couponService.validateCoupon(code);
            return ResponseEntity.ok(new ApiResponseDTO<Object>(true, "coupons validated successfully.", isValid));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<Object>(false, e.getMessage(), null));
        }
    }

    // Get all coupons (public)
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<Coupon>>> getAllCoupons() {
        try {
            List<Coupon> coupons = couponService.getAllCoupons();
            return ResponseEntity.ok(new ApiResponseDTO<List<Coupon>>(true, "coupons fetched successfully.", coupons));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<List<Coupon>>(false, e.getMessage(), null));
        }
       
    }

    // Update a coupon (admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Coupon>> updateCoupon(
            @PathVariable Long id,
            @RequestBody Coupon coupon) {
        try {
            Coupon updatedCoupon = couponService.updateCoupon(id, coupon);
            return ResponseEntity.ok(new ApiResponseDTO<Coupon>(true, "Coupon updated successfully", updatedCoupon));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<Coupon>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<Coupon>(false, e.getMessage(), null));
        }
    }

    // Delete a coupon by ID (admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> deleteCoupon(@PathVariable Long id) {
        try {
            couponService.deleteCoupon(id);
            return ResponseEntity.ok(new ApiResponseDTO<Void>(true, "Coupon deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<Void>(false, e.getMessage(), null));
        }
    }
}