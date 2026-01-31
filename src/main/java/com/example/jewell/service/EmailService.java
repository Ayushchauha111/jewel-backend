package com.example.jewell.service;

import com.example.jewell.model.Billing;
import com.example.jewell.model.BillingItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private BillPdfService billPdfService;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("₹#,##0.00", new DecimalFormatSymbols(Locale.ENGLISH));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public void sendBillEmail(Billing billing, String receiptType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(billing.getCustomer().getEmail());
            boolean isGst = "GST".equalsIgnoreCase(receiptType);
            helper.setSubject((isGst ? "Tax Invoice - " : "Bill Receipt - ") + billing.getBillNumber());

            String emailBody = isGst ? generateGstEmailBody(billing) : generateBillEmailBody(billing);
            helper.setText(emailBody, true);

            byte[] pdfBytes = billPdfService.generatePdf(billing, receiptType);
            String pdfFilename = "Bill-" + billing.getBillNumber().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
            helper.addAttachment(pdfFilename, new ByteArrayDataSource(pdfBytes, "application/pdf"));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email", e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return CURRENCY_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "—";
        return date.format(DATE_FORMAT);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Normal receipt – same structure as frontend NormalReceipt: table Item, Article Code, Carat, Diamond Ct, Qty, Rate (₹/g), Total; gold + diamond rows when applicable; totals Gold, Diamond, Total (Gold+Diamond), Discount, Total, Paid. */
    private String generateBillEmailBody(Billing billing) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>");
        html.append("body{font-family:Arial,sans-serif;margin:20px;} table{border-collapse:collapse;width:100%;margin:16px 0;} th,td{border:1px solid #ddd;padding:8px;text-align:left;} th{background:#f2f2f2;} .totals{margin-top:16px;} .totals p{display:flex;justify-content:space-between;margin:6px 0;}");
        html.append("</style></head><body>");
        html.append("<h2>Receipt</h2>");
        html.append("<p><strong>Bill No:</strong> ").append(escape(billing.getBillNumber())).append("</p>");
        html.append("<p><strong>Date:</strong> ").append(formatDate(billing.getCreatedAt())).append("</p>");
        html.append("<p><strong>Customer:</strong> ").append(escape(billing.getCustomer().getName())).append("</p>");
        if (billing.getCustomer().getPhone() != null && !billing.getCustomer().getPhone().isEmpty()) {
            html.append("<p><strong>Phone:</strong> ").append(escape(billing.getCustomer().getPhone())).append("</p>");
        }
        html.append("<table><thead><tr><th>Item</th><th>Article Code</th><th>Carat</th><th>Diamond Ct</th><th>Qty</th><th>Rate (₹/g)</th><th>Total</th></tr></thead><tbody>");

        for (BillingItem item : billing.getItems()) {
            String articleCode = item.getArticleCode();
            if ((articleCode == null || articleCode.isEmpty()) && item.getStock() != null) {
                articleCode = item.getStock().getArticleCode();
            }
            String itemName = item.getItemName() != null ? item.getItemName() : (item.getStock() != null ? item.getStock().getArticleName() : "—");
            BigDecimal wt = item.getWeightGrams() != null ? item.getWeightGrams() : (item.getStock() != null ? item.getStock().getWeightGrams() : null);
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal diamondAmt = item.getDiamondAmount() != null ? item.getDiamondAmount() : BigDecimal.ZERO;
            BigDecimal metalAmt = (item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO).subtract(diamondAmt);
            BigDecimal gswt = wt != null ? wt.multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;
            BigDecimal ratePerGram = (gswt.compareTo(BigDecimal.ZERO) > 0) ? metalAmt.divide(gswt, 2, RoundingMode.HALF_UP) : null;
            BigDecimal carat = item.getCarat() != null ? item.getCarat() : (item.getStock() != null ? item.getStock().getCarat() : null);
            BigDecimal diamondCt = item.getDiamondCarat() != null ? item.getDiamondCarat() : (item.getStock() != null ? item.getStock().getDiamondCarat() : null);
            BigDecimal unitPrice = item.getUnitPrice();

            if (diamondAmt.compareTo(BigDecimal.ZERO) > 0) {
                html.append("<tr><td>").append(escape(itemName)).append(" (Gold)</td><td>").append(escape(articleCode != null ? articleCode : "—")).append("</td><td>").append(carat != null ? carat : "—").append("</td><td>—</td><td>").append(qty).append("</td><td>").append(ratePerGram != null ? formatCurrency(ratePerGram) : "—").append("</td><td>").append(formatCurrency(metalAmt)).append("</td></tr>");
                html.append("<tr><td>Diamond</td><td>—</td><td>—</td><td>").append(diamondCt != null ? diamondCt.multiply(BigDecimal.valueOf(qty)).setScale(3, RoundingMode.HALF_UP) : "—").append("</td><td>").append(qty).append("</td><td>—</td><td>").append(formatCurrency(diamondAmt)).append("</td></tr>");
            } else {
                BigDecimal rate = (wt != null && wt.compareTo(BigDecimal.ZERO) > 0 && unitPrice != null) ? unitPrice.divide(wt, 2, RoundingMode.HALF_UP) : null;
                html.append("<tr><td>").append(escape(itemName)).append("</td><td>").append(escape(articleCode != null ? articleCode : "—")).append("</td><td>").append(carat != null ? carat : "—").append("</td><td>").append(diamondCt != null ? diamondCt : "—").append("</td><td>").append(qty).append("</td><td>").append(rate != null ? formatCurrency(rate) : "—").append("</td><td>").append(formatCurrency(item.getTotalPrice())).append("</td></tr>");
            }
        }
        html.append("</tbody></table>");

        html.append("<div class=\"totals\">");
        BigDecimal totalDiamond = billing.getTotalDiamondAmount() != null ? billing.getTotalDiamondAmount() : BigDecimal.ZERO;
        if (totalDiamond.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalAmt = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
            html.append("<p><span>Gold</span><span>").append(formatCurrency(totalAmt.subtract(totalDiamond))).append("</span></p>");
            html.append("<p><span>Diamond</span><span>").append(formatCurrency(totalDiamond)).append("</span></p>");
            html.append("<p><span>Total (Gold + Diamond)</span><span>").append(formatCurrency(totalAmt)).append("</span></p>");
        } else {
            html.append("<p><span>Subtotal</span><span>").append(formatCurrency(billing.getTotalAmount())).append("</span></p>");
        }
        html.append("<p><span>Discount</span><span>-").append(formatCurrency(billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO)).append("</span></p>");
        html.append("<p><span><strong>Total</strong></span><span><strong>").append(formatCurrency(billing.getFinalAmount())).append("</strong></span></p>");
        if (billing.getPaidAmount() != null && billing.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<p><span>Paid</span><span>").append(formatCurrency(billing.getPaidAmount())).append("</span></p>");
        }
        html.append("</div>");
        if (billing.getNotes() != null && !billing.getNotes().isEmpty()) {
            html.append("<p><strong>Notes:</strong> ").append(escape(billing.getNotes())).append("</p>");
        }
        html.append("<p>Thank you for your purchase!</p>");
        html.append("</body></html>");
        return html.toString();
    }

    /** GST invoice – same structure as frontend GSTReceipt: Prod ID, Design, Desc, Carat, Diamond Ct, Qty, GSWT, NT WT, Rate, MKG, DIA Val, Amount; gold + diamond rows; Total Gross Wt, Gold, Diamond, Total; Payment; Taxation; Amount in words; Terms. */
    private String generateGstEmailBody(Billing billing) {
        BigDecimal totalAmount = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal totalDiamondAmt = billing.getTotalDiamondAmount() != null ? billing.getTotalDiamondAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = billing.getFinalAmount() != null ? billing.getFinalAmount() : BigDecimal.ZERO;
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = finalAmount;
        double gstRate = 0.03;
        BigDecimal cgstAmount = taxableAmount.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = taxableAmount.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGst = cgstAmount.add(sgstAmount);
        BigDecimal grandTotal = taxableAmount.add(totalGst);
        BigDecimal roundOff = grandTotal.setScale(0, RoundingMode.HALF_UP).subtract(grandTotal);
        BigDecimal totalGrossWeight = BigDecimal.ZERO;
        for (BillingItem i : billing.getItems()) {
            BigDecimal w = i.getWeightGrams() != null ? i.getWeightGrams() : BigDecimal.ZERO;
            int q = i.getQuantity() != null ? i.getQuantity() : 1;
            totalGrossWeight = totalGrossWeight.add(w.multiply(BigDecimal.valueOf(q)));
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>");
        html.append("body{font-family:Arial,sans-serif;margin:20px;} table{border-collapse:collapse;width:100%;margin:12px 0;} th,td{border:1px solid #ddd;padding:6px;text-align:left;} th{background:#f2f2f2;} .billto,.payment,.tax{margin:16px 0;} .sig{display:inline-block;margin-right:40px;}");
        html.append("</style></head><body>");
        html.append("<h2>Tax Invoice</h2>");
        html.append("<p><strong>Invoice No:</strong> ").append(escape(billing.getBillNumber())).append("</p>");
        html.append("<p><strong>Date:</strong> ").append(formatDate(billing.getCreatedAt())).append("</p>");
        html.append("<div class=\"billto\"><h3>Bill To</h3>");
        html.append("<p><strong>").append(escape(billing.getCustomer().getName())).append("</strong></p>");
        if (billing.getCustomer().getPhone() != null) html.append("<p>Phone: ").append(escape(billing.getCustomer().getPhone())).append("</p>");
        if (billing.getCustomer().getEmail() != null) html.append("<p>Email: ").append(escape(billing.getCustomer().getEmail())).append("</p>");
        html.append("</div>");

        html.append("<table><thead><tr><th>Prod ID</th><th>Design</th><th>Desc</th><th>Carat</th><th>Diamond Ct</th><th>Qty</th><th>GSWT (g)</th><th>NT WT (g)</th><th>Rate</th><th>MKG</th><th>DIA Val</th><th>Amount</th></tr></thead><tbody>");
        for (BillingItem item : billing.getItems()) {
            String articleCode = item.getArticleCode();
            if ((articleCode == null || articleCode.isEmpty()) && item.getStock() != null) {
                articleCode = item.getStock().getArticleCode();
            }
            String itemName = item.getItemName() != null ? item.getItemName() : (item.getStock() != null ? item.getStock().getArticleName() : "—");
            BigDecimal wt = item.getWeightGrams() != null ? item.getWeightGrams() : BigDecimal.ZERO;
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal gswt = wt.multiply(BigDecimal.valueOf(qty));
            BigDecimal diamondAmt = item.getDiamondAmount() != null ? item.getDiamondAmount() : BigDecimal.ZERO;
            BigDecimal metalAmt = (item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO).subtract(diamondAmt);
            BigDecimal ratePerGramGold = gswt.compareTo(BigDecimal.ZERO) > 0 ? metalAmt.divide(gswt, 2, RoundingMode.HALF_UP) : null;
            BigDecimal carat = item.getCarat() != null ? item.getCarat() : (item.getStock() != null ? item.getStock().getCarat() : null);
            BigDecimal diamondCt = item.getDiamondCarat() != null ? item.getDiamondCarat() : (item.getStock() != null ? item.getStock().getDiamondCarat() : null);
            String imageUrl = item.getStock() != null ? item.getStock().getImageUrl() : null;

            if (diamondAmt.compareTo(BigDecimal.ZERO) > 0) {
                html.append("<tr><td>").append(escape(articleCode != null ? articleCode : "—")).append("</td><td>").append(imageUrl != null ? "<img src=\"" + escape(imageUrl) + "\" alt=\"\" style=\"max-width:60px;max-height:60px;\" />" : "—").append("</td><td>").append(escape(itemName)).append(" (Gold)</td><td>").append(carat != null ? carat : "—").append("</td><td>—</td><td>").append(qty).append("</td><td>").append(gswt.setScale(3, RoundingMode.HALF_UP)).append("</td><td>").append(gswt.setScale(3, RoundingMode.HALF_UP)).append("</td><td>").append(ratePerGramGold != null ? formatCurrency(ratePerGramGold) : "—").append("</td><td>—</td><td>—</td><td>").append(formatCurrency(metalAmt)).append("</td></tr>");
                html.append("<tr><td>—</td><td>—</td><td>Diamond</td><td>—</td><td>").append(diamondCt != null ? diamondCt.multiply(BigDecimal.valueOf(qty)).setScale(3, RoundingMode.HALF_UP) : "—").append("</td><td>").append(qty).append("</td><td>—</td><td>—</td><td>—</td><td>—</td><td>—</td><td>").append(formatCurrency(diamondAmt)).append("</td></tr>");
            } else {
                BigDecimal ratePerGram = wt.compareTo(BigDecimal.ZERO) > 0 && item.getUnitPrice() != null ? item.getUnitPrice().divide(wt, 2, RoundingMode.HALF_UP) : null;
                html.append("<tr><td>").append(escape(articleCode != null ? articleCode : "—")).append("</td><td>").append(imageUrl != null ? "<img src=\"" + escape(imageUrl) + "\" alt=\"\" style=\"max-width:60px;max-height:60px;\" />" : "—").append("</td><td>").append(escape(itemName)).append("</td><td>").append(carat != null ? carat : "—").append("</td><td>").append(diamondCt != null ? diamondCt : "—").append("</td><td>").append(qty).append("</td><td>").append(gswt.setScale(3, RoundingMode.HALF_UP)).append("</td><td>").append(gswt.setScale(3, RoundingMode.HALF_UP)).append("</td><td>").append(ratePerGram != null ? formatCurrency(ratePerGram) : (item.getUnitPrice() != null ? formatCurrency(item.getUnitPrice()) : "—")).append("</td><td>—</td><td>—</td><td>").append(formatCurrency(item.getTotalPrice())).append("</td></tr>");
            }
        }
        html.append("</tbody></table>");

        html.append("<p><strong>Total Gross Wt:</strong> ").append(totalGrossWeight.setScale(3, RoundingMode.HALF_UP)).append(" g &nbsp; <strong>Total Net Wt:</strong> ").append(totalGrossWeight.setScale(3, RoundingMode.HALF_UP)).append(" g</p>");
        html.append("<p><strong>Gold:</strong> ").append(formatCurrency(totalAmount.subtract(totalDiamondAmt))).append("</p>");
        if (totalDiamondAmt.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<p><strong>Diamond:</strong> ").append(formatCurrency(totalDiamondAmt)).append("</p>");
        }
        html.append("<p><strong>Total:</strong> ").append(formatCurrency(finalAmount)).append("</p>");
        html.append("<div class=\"payment\"><h3>Payment Details</h3>");
        html.append("<p><strong>Payment Method:</strong> ").append(escape(billing.getPaymentMethod() != null ? billing.getPaymentMethod().name() : "—")).append("</p>");
        html.append("<p><strong>Total Received Amount:</strong> ").append(formatCurrency(paidAmount)).append("</p></div>");
        html.append("<div class=\"tax\"><h3>Taxation Summary</h3>");
        html.append("<table><tr><td>Taxable Amount</td><td>").append(formatCurrency(taxableAmount)).append("</td></tr>");
        html.append("<tr><td>CGST (3%)</td><td>").append(formatCurrency(cgstAmount)).append("</td></tr>");
        html.append("<tr><td>SGST (3%)</td><td>").append(formatCurrency(sgstAmount)).append("</td></tr>");
        html.append("<tr><td>Round Off</td><td>").append(formatCurrency(roundOff)).append("</td></tr>");
        html.append("<tr><td><strong>Total Amount</strong></td><td><strong>").append(formatCurrency(grandTotal.add(roundOff))).append("</strong></td></tr>");
        html.append("<tr><td>Net Received Amount</td><td>").append(formatCurrency(paidAmount)).append("</td></tr>");
        html.append("<tr><td>Closing Balance</td><td>").append(formatCurrency(grandTotal.add(roundOff).subtract(paidAmount))).append("</td></tr></table></div>");
        html.append("<p><strong>Amount in Words:</strong> ").append(escape(amountInWords(grandTotal.add(roundOff).longValue()))).append("</p>");
        html.append("<h3>Terms &amp; Conditions</h3>");
        html.append("<p>Goods once sold will not be taken back or exchanged. Price of the product is as per the rate prevailing at the time of purchase. Payment is required in full at the time of purchase unless otherwise agreed.</p>");
        html.append("<p>Thank you for your purchase!</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
    private static final String[] TEENS = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};

    private String amountInWords(long n) {
        if (n <= 0) return "Zero Only";
        long num = n;
        StringBuilder sb = new StringBuilder();
        if (num >= 1_00_00_000) { sb.append(toWords3((int)(num / 1_00_00_000))).append(" Crore "); num %= 1_00_00_000; }
        if (num >= 1_00_000) { sb.append(toWords3((int)(num / 1_00_000))).append(" Lakh "); num %= 1_00_000; }
        if (num >= 1000) { sb.append(toWords3((int)(num / 1000))).append(" Thousand "); num %= 1000; }
        if (num > 0) sb.append(toWords3((int)num));
        return sb.toString().trim().replaceAll("\\s+", " ") + " Only";
    }

    private String toWords3(int n) {
        if (n == 0) return "";
        int h = n / 100, r = n % 100;
        String part = h > 0 ? ONES[h] + " Hundred" : "";
        return (part + (r > 0 ? " " + toWords2(r) : "")).trim();
    }

    private String toWords2(int n) {
        if (n < 10) return ONES[n];
        if (n < 20) return TEENS[n - 10];
        return TENS[n / 10] + (n % 10 > 0 ? " " + ONES[n % 10] : "");
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
