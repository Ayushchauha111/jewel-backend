package com.example.jewell.service;

import com.example.jewell.model.Billing;
import com.example.jewell.model.BillingItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppService {
    
    @Value("${whatsapp.api.url:https://api.whatsapp.com/send}")
    private String whatsappApiUrl;

    public void sendBillWhatsApp(Billing billing) {
        String phoneNumber = billing.getCustomer().getWhatsappNumber();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            phoneNumber = billing.getCustomer().getPhone();
        }
        
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new RuntimeException("Customer phone number not found");
        }

        String message = generateBillMessage(billing);
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String whatsappUrl = String.format("%s?phone=%s&text=%s", 
            whatsappApiUrl, 
            phoneNumber.replaceAll("[^0-9]", ""), 
            encodedMessage);
        
        // In a real implementation, you would use WhatsApp Business API
        // For now, this generates a WhatsApp link that can be opened
        // You can integrate with Twilio, WhatsApp Business API, or other services
        System.out.println("WhatsApp URL: " + whatsappUrl);
    }

    private String generateBillMessage(Billing billing) {
        StringBuilder message = new StringBuilder();
        message.append("🏪 *Bill Receipt*\n\n");
        message.append("Bill Number: ").append(billing.getBillNumber()).append("\n");
        message.append("Customer: ").append(billing.getCustomer().getName()).append("\n");
        message.append("Date: ").append(billing.getCreatedAt()).append("\n\n");
        message.append("*Items:*\n");
        
        for (BillingItem item : billing.getItems()) {
            message.append("• ").append(item.getItemName() != null ? item.getItemName() : "-");
            if (item.getArticleCode() != null && !item.getArticleCode().isEmpty()) {
                message.append(" (").append(item.getArticleCode()).append(")");
            }
            message.append(" - Weight: ").append(item.getWeightGrams()).append("g")
                   .append(", Carat: ").append(item.getCarat())
                   .append(", Qty: ").append(item.getQuantity())
                   .append(" - ₹").append(item.getTotalPrice()).append("\n");
        }
        
        message.append("\nTotal: ₹").append(billing.getTotalAmount());
        if (billing.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            message.append("\nDiscount: ₹").append(billing.getDiscountAmount());
        }
        message.append("\n*Final Amount: ₹").append(billing.getFinalAmount()).append("*");
        message.append("\nPayment: ").append(billing.getPaymentMethod());
        
        return message.toString();
    }
}
