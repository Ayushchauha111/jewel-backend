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

    public void sendBillWhatsApp(Billing billing, String receiptType) {
        String phoneNumber = billing.getCustomer().getWhatsappNumber();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            phoneNumber = billing.getCustomer().getPhone();
        }
        
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new RuntimeException("Customer phone number not found");
        }

        boolean isGst = "GST".equalsIgnoreCase(receiptType);
        String message = generateBillMessage(billing, isGst);
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String whatsappUrl = String.format("%s?phone=%s&text=%s", 
            whatsappApiUrl, 
            phoneNumber.replaceAll("[^0-9]", ""), 
            encodedMessage);
        
        // In a real implementation, you would use WhatsApp Business API
        // For now, this generates a WhatsApp link that can be opened
        System.out.println("WhatsApp URL: " + whatsappUrl);
    }

    private String generateBillMessage(Billing billing, boolean isGst) {
        StringBuilder message = new StringBuilder();
        message.append(isGst ? "📄 *Tax Invoice*\n\n" : "🏪 *Bill Receipt*\n\n");
        message.append("Bill No: ").append(billing.getBillNumber()).append("\n");
        message.append("Customer: ").append(billing.getCustomer().getName()).append("\n");
        message.append("Date: ").append(billing.getCreatedAt()).append("\n\n");
        message.append("*Items:*\n");
        
        for (BillingItem item : billing.getItems()) {
            message.append("• ").append(item.getItemName() != null ? item.getItemName() : "-");
            if (item.getArticleCode() != null && !item.getArticleCode().isEmpty()) {
                message.append(" (").append(item.getArticleCode()).append(")");
            }
            message.append(" - Wt: ").append(item.getWeightGrams() != null ? item.getWeightGrams() : "-").append("g");
            message.append(", Carat: ").append(item.getCarat() != null ? item.getCarat() : "-");
            if (item.getDiamondCarat() != null) {
                message.append(", Dia: ").append(item.getDiamondCarat()).append(" ct");
            }
            message.append(", Qty: ").append(item.getQuantity());
            message.append(" - ₹").append(item.getTotalPrice()).append("\n");
        }
        
        message.append("\nSubtotal: ₹").append(billing.getTotalAmount());
        if (billing.getTotalDiamondAmount() != null && billing.getTotalDiamondAmount().compareTo(BigDecimal.ZERO) > 0) {
            message.append("\nGold/Metal: ₹").append(billing.getTotalAmount().subtract(billing.getTotalDiamondAmount()));
            message.append("\nDiamond: ₹").append(billing.getTotalDiamondAmount());
        }
        if (billing.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            message.append("\nDiscount: ₹").append(billing.getDiscountAmount());
        }
        message.append("\n*Total: ₹").append(billing.getFinalAmount()).append("*");
        message.append("\nPayment: ").append(billing.getPaymentMethod());
        
        return message.toString();
    }
}
