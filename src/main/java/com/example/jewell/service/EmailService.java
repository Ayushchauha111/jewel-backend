package com.example.jewell.service;

import com.example.jewell.model.Billing;
import com.example.jewell.model.BillingItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendBillEmail(Billing billing) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(billing.getCustomer().getEmail());
            helper.setSubject("Bill Receipt - " + billing.getBillNumber());

            String emailBody = generateBillEmailBody(billing);
            helper.setText(emailBody, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email", e);
        }
    }

    private String generateBillEmailBody(Billing billing) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body { font-family: Arial, sans-serif; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append("</style></head><body>");
        html.append("<h2>Bill Receipt</h2>");
        html.append("<p><strong>Bill Number:</strong> ").append(billing.getBillNumber()).append("</p>");
        html.append("<p><strong>Customer:</strong> ").append(billing.getCustomer().getName()).append("</p>");
        html.append("<p><strong>Date:</strong> ").append(billing.getCreatedAt()).append("</p>");
        
        html.append("<table>");
        html.append("<tr><th>Item</th><th>Article Code</th><th>Weight (g)</th><th>Carat</th><th>Quantity</th><th>Price</th></tr>");
        
        for (BillingItem item : billing.getItems()) {
            String articleCode = item.getArticleCode();
            if ((articleCode == null || articleCode.isEmpty()) && item.getStock() != null) {
                articleCode = item.getStock().getArticleCode();
            }
            html.append("<tr>");
            html.append("<td>").append(item.getItemName() != null ? item.getItemName() : "").append("</td>");
            html.append("<td>").append(articleCode != null ? articleCode : "").append("</td>");
            html.append("<td>").append(item.getWeightGrams()).append("</td>");
            html.append("<td>").append(item.getCarat()).append("</td>");
            html.append("<td>").append(item.getQuantity()).append("</td>");
            html.append("<td>").append(item.getTotalPrice()).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</table>");
        html.append("<p><strong>Total Amount:</strong> ₹").append(billing.getTotalAmount()).append("</p>");
        if (billing.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<p><strong>Discount:</strong> ₹").append(billing.getDiscountAmount()).append("</p>");
        }
        html.append("<p><strong>Final Amount:</strong> ₹").append(billing.getFinalAmount()).append("</p>");
        html.append("<p><strong>Payment Method:</strong> ").append(billing.getPaymentMethod()).append("</p>");
        html.append("</body></html>");
        
        return html.toString();
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email", e);
        }
    }

    public void sendOtpEmail(String to, String otp) {
        String subject = "Your OTP Code";
        String body = "<html><body><h2>Your OTP Code</h2><p>Your OTP code is: <strong>" + otp + "</strong></p><p>This code will expire in 10 minutes.</p></body></html>";
        sendEmail(to, subject, body);
    }

    public int sendBulkEmail(java.util.Map<String, com.example.jewell.model.User> users, String subject, java.util.List<String> ccList, String body, String from) {
        int count = 0;
        try {
            for (com.example.jewell.model.User user : users.values()) {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    sendEmail(user.getEmail(), subject, body);
                    count++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending bulk email", e);
        }
        return count;
    }

    public int sendBulkEmail(java.util.List<String> emailList, String subject, String body) {
        int count = 0;
        try {
            for (String email : emailList) {
                if (email != null && !email.isEmpty()) {
                    sendEmail(email, subject, body);
                    count++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending bulk email", e);
        }
        return count;
    }

    public void sendEmail(com.example.jewell.model.User user, String subject, String body, String from, String replyTo) {
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            sendEmail(user.getEmail(), subject, body);
        }
    }
}
