package com.example.jewell.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.example.jewell.dto.PaymentRequestDTO;
import com.example.jewell.dto.PaymentVerificationRequestDTO;
import com.example.jewell.utils.RazorpayUtils;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    @Value("${jewell.app.razorpay.key}")
    private String RAZORPAY_KEY_ID;

    @Value("${jewell.app.razorpay.secret}")
    private String RAZORPAY_KEY_SECRET;

    // Create an order
    @PostMapping("/create-order")
    public String createOrder(@RequestBody PaymentRequestDTO paymentRequest) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", paymentRequest.getAmount() * 100); // Amount in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_receipt_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(orderRequest);
        return order.toString();
    }

    // Verify payment
    @PostMapping("/verify-payment")
    public String verifyPayment(@RequestBody PaymentVerificationRequestDTO verificationRequest) {
        try {
            String generatedSignature = RazorpayUtils.generateSignature(verificationRequest.getOrderId() + "|" + verificationRequest.getPaymentId(), RAZORPAY_KEY_SECRET);

            if (generatedSignature.equals(verificationRequest.getSignature())) {
                return "Payment successful!";
            } else {
                return "Payment verification failed!";
            }
        } catch (Exception e) {
            return "Error verifying payment: " + e.getMessage();
        }
    }
}