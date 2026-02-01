package com.example.jewell.service;

import com.example.jewell.model.Billing;
import com.example.jewell.model.BillingItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BillPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "—";
        return date.format(DATE_FORMAT);
    }

    /**
     * Generate PDF bytes for the bill (Normal receipt or GST invoice).
     */
    public byte[] generatePdf(Billing billing, String receiptType) {
        boolean isGst = "GST".equalsIgnoreCase(receiptType);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            if (isGst) {
                buildGstPdf(document, billing);
            } else {
                buildNormalPdf(document, billing);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate bill PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void buildNormalPdf(Document document, Billing billing) throws DocumentException {
        document.add(new Paragraph("Receipt", TITLE_FONT));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Bill No: " + billing.getBillNumber(), NORMAL_FONT));
        document.add(new Paragraph("Date: " + formatDate(billing.getCreatedAt()), NORMAL_FONT));
        document.add(new Paragraph("Customer: " + (billing.getCustomer() != null ? billing.getCustomer().getName() : "—"), NORMAL_FONT));
        if (billing.getCustomer() != null && billing.getCustomer().getPhone() != null) {
            document.add(new Paragraph("Phone: " + billing.getCustomer().getPhone(), NORMAL_FONT));
        }
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{2.5f, 1.5f, 0.8f, 1f, 0.5f, 1.2f, 1.2f});
        String[] headers = {"Item", "Article Code", "Carat", "Diamond Ct", "Qty", "Rate (₹/g)", "Total"};
        for (String h : headers) addCell(table, h, HEADER_FONT);

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
                addCell(table, itemName + " (Gold)", NORMAL_FONT);
                addCell(table, articleCode != null ? articleCode : "—", NORMAL_FONT);
                addCell(table, carat != null ? carat.toString() : "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, String.valueOf(qty), NORMAL_FONT);
                addCell(table, ratePerGram != null ? formatCurrency(ratePerGram) : "—", NORMAL_FONT);
                addCell(table, formatCurrency(metalAmt), NORMAL_FONT);
                addCell(table, "Diamond", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, diamondCt != null ? diamondCt.multiply(BigDecimal.valueOf(qty)).setScale(3, RoundingMode.HALF_UP).toString() : "—", NORMAL_FONT);
                addCell(table, String.valueOf(qty), NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, formatCurrency(diamondAmt), NORMAL_FONT);
            } else {
                BigDecimal rate = (wt != null && wt.compareTo(BigDecimal.ZERO) > 0 && unitPrice != null) ? unitPrice.divide(wt, 2, RoundingMode.HALF_UP) : null;
                addCell(table, itemName, NORMAL_FONT);
                addCell(table, articleCode != null ? articleCode : "—", NORMAL_FONT);
                addCell(table, carat != null ? carat.toString() : "—", NORMAL_FONT);
                addCell(table, diamondCt != null ? diamondCt.toString() : "—", NORMAL_FONT);
                addCell(table, String.valueOf(qty), NORMAL_FONT);
                addCell(table, rate != null ? formatCurrency(rate) : "—", NORMAL_FONT);
                addCell(table, formatCurrency(item.getTotalPrice()), NORMAL_FONT);
            }
        }
        document.add(table);
        document.add(new Paragraph(" "));

        BigDecimal totalDiamond = billing.getTotalDiamondAmount() != null ? billing.getTotalDiamondAmount() : BigDecimal.ZERO;
        if (totalDiamond.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalAmt = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
            document.add(new Paragraph("Gold: " + formatCurrency(totalAmt.subtract(totalDiamond)), NORMAL_FONT));
            document.add(new Paragraph("Diamond: " + formatCurrency(totalDiamond), NORMAL_FONT));
            document.add(new Paragraph("Total (Gold + Diamond): " + formatCurrency(totalAmt), NORMAL_FONT));
        } else {
            document.add(new Paragraph("Subtotal: " + formatCurrency(billing.getTotalAmount()), NORMAL_FONT));
        }
        document.add(new Paragraph("Discount: -" + formatCurrency(billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO), NORMAL_FONT));
        document.add(new Paragraph("Making Charges: " + formatCurrency(billing.getMakingCharges() != null ? billing.getMakingCharges() : BigDecimal.ZERO), NORMAL_FONT));
        document.add(new Paragraph("Total: " + formatCurrency(billing.getFinalAmount()), HEADER_FONT));
        if (billing.getPaidAmount() != null && billing.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            document.add(new Paragraph("Paid: " + formatCurrency(billing.getPaidAmount()), NORMAL_FONT));
        }
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Thank you for your purchase!", NORMAL_FONT));
    }

    private void buildGstPdf(Document document, Billing billing) throws DocumentException {
        document.add(new Paragraph("Tax Invoice", TITLE_FONT));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Invoice No: " + billing.getBillNumber(), NORMAL_FONT));
        document.add(new Paragraph("Date: " + formatDate(billing.getCreatedAt()), NORMAL_FONT));
        document.add(new Paragraph("Bill To: " + (billing.getCustomer() != null ? billing.getCustomer().getName() : "—"), NORMAL_FONT));
        if (billing.getCustomer() != null) {
            if (billing.getCustomer().getPhone() != null) document.add(new Paragraph("Phone: " + billing.getCustomer().getPhone(), NORMAL_FONT));
            if (billing.getCustomer().getEmail() != null) document.add(new Paragraph("Email: " + billing.getCustomer().getEmail(), NORMAL_FONT));
        }
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(12);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1f, 1f, 2f, 0.6f, 0.8f, 0.5f, 0.8f, 0.8f, 1f, 0.6f, 0.8f, 1f});
        String[] headers = {"Prod ID", "Design", "Desc", "Carat", "Dia Ct", "Qty", "GSWT(g)", "NT(g)", "Rate", "MKG", "DIA Val", "Amount"};
        for (String h : headers) addCell(table, h, HEADER_FONT);

        BigDecimal totalGrossWeight = BigDecimal.ZERO;
        for (BillingItem item : billing.getItems()) {
            String articleCode = item.getArticleCode();
            if ((articleCode == null || articleCode.isEmpty()) && item.getStock() != null) {
                articleCode = item.getStock().getArticleCode();
            }
            String itemName = item.getItemName() != null ? item.getItemName() : (item.getStock() != null ? item.getStock().getArticleName() : "—");
            BigDecimal wt = item.getWeightGrams() != null ? item.getWeightGrams() : BigDecimal.ZERO;
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal gswt = wt.multiply(BigDecimal.valueOf(qty));
            totalGrossWeight = totalGrossWeight.add(gswt);
            BigDecimal diamondAmt = item.getDiamondAmount() != null ? item.getDiamondAmount() : BigDecimal.ZERO;
            BigDecimal metalAmt = (item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO).subtract(diamondAmt);
            BigDecimal ratePerGramGold = gswt.compareTo(BigDecimal.ZERO) > 0 ? metalAmt.divide(gswt, 2, RoundingMode.HALF_UP) : null;
            BigDecimal carat = item.getCarat() != null ? item.getCarat() : (item.getStock() != null ? item.getStock().getCarat() : null);
            BigDecimal diamondCt = item.getDiamondCarat() != null ? item.getDiamondCarat() : (item.getStock() != null ? item.getStock().getDiamondCarat() : null);
            BigDecimal unitPrice = item.getUnitPrice();

            if (diamondAmt.compareTo(BigDecimal.ZERO) > 0) {
                addCell(table, articleCode != null ? articleCode : "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, itemName + " (Gold)", NORMAL_FONT);
                addCell(table, carat != null ? carat.toString() : "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, String.valueOf(qty), NORMAL_FONT);
                addCell(table, gswt.setScale(3, RoundingMode.HALF_UP).toString(), NORMAL_FONT);
                addCell(table, gswt.setScale(3, RoundingMode.HALF_UP).toString(), NORMAL_FONT);
                addCell(table, ratePerGramGold != null ? formatCurrency(ratePerGramGold) : "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, formatCurrency(metalAmt), NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "Diamond", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, diamondCt != null ? diamondCt.multiply(BigDecimal.valueOf(qty)).setScale(3, RoundingMode.HALF_UP).toString() : "—", NORMAL_FONT);
                addCell(table, String.valueOf(qty), NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, formatCurrency(diamondAmt), NORMAL_FONT);
            } else {
                BigDecimal ratePerGram = wt.compareTo(BigDecimal.ZERO) > 0 && unitPrice != null ? unitPrice.divide(wt, 2, RoundingMode.HALF_UP) : null;
                addCell(table, articleCode != null ? articleCode : "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, itemName, NORMAL_FONT);
                addCell(table, carat != null ? carat.toString() : "—", NORMAL_FONT);
                addCell(table, diamondCt != null ? diamondCt.toString() : "—", NORMAL_FONT);
                addCell(table, String.valueOf(qty), NORMAL_FONT);
                addCell(table, gswt.setScale(3, RoundingMode.HALF_UP).toString(), NORMAL_FONT);
                addCell(table, gswt.setScale(3, RoundingMode.HALF_UP).toString(), NORMAL_FONT);
                addCell(table, ratePerGram != null ? formatCurrency(ratePerGram) : (unitPrice != null ? formatCurrency(unitPrice) : "—"), NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, "—", NORMAL_FONT);
                addCell(table, formatCurrency(item.getTotalPrice()), NORMAL_FONT);
            }
        }
        document.add(table);
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Total Gross Wt: " + totalGrossWeight.setScale(3, RoundingMode.HALF_UP) + " g", NORMAL_FONT));
        BigDecimal totalAmount = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal totalDiamondAmt = billing.getTotalDiamondAmount() != null ? billing.getTotalDiamondAmount() : BigDecimal.ZERO;
        document.add(new Paragraph("Gold: " + formatCurrency(totalAmount.subtract(totalDiamondAmt)), NORMAL_FONT));
        if (totalDiamondAmt.compareTo(BigDecimal.ZERO) > 0) {
            document.add(new Paragraph("Diamond: " + formatCurrency(totalDiamondAmt), NORMAL_FONT));
        }
        BigDecimal discount = billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal makingCharges = billing.getMakingCharges() != null ? billing.getMakingCharges() : BigDecimal.ZERO;
        document.add(new Paragraph("Subtotal: " + formatCurrency(totalAmount), NORMAL_FONT));
        document.add(new Paragraph("Discount: -" + formatCurrency(discount), NORMAL_FONT));
        document.add(new Paragraph("Making Charges: " + formatCurrency(makingCharges), NORMAL_FONT));
        document.add(new Paragraph("Total: " + formatCurrency(billing.getFinalAmount()), HEADER_FONT));
        document.add(new Paragraph(" "));
        BigDecimal finalAmt = billing.getFinalAmount() != null ? billing.getFinalAmount() : BigDecimal.ZERO;
        double gstRate = 0.015;
        BigDecimal cgst = finalAmt.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = finalAmt.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGst = cgst.add(sgst);
        BigDecimal grandTotal = finalAmt.add(totalGst);
        document.add(new Paragraph("Taxable Amount: " + formatCurrency(finalAmt), NORMAL_FONT));
        document.add(new Paragraph("CGST (1.5%): " + formatCurrency(cgst), NORMAL_FONT));
        document.add(new Paragraph("SGST (1.5%): " + formatCurrency(sgst), NORMAL_FONT));
        document.add(new Paragraph("GST (3%): " + formatCurrency(totalGst), NORMAL_FONT));
        document.add(new Paragraph("Total (incl. GST): " + formatCurrency(grandTotal), HEADER_FONT));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Payment Method: " + (billing.getPaymentMethod() != null ? billing.getPaymentMethod().name() : "—"), NORMAL_FONT));
        document.add(new Paragraph("Total Received: " + formatCurrency(billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO), NORMAL_FONT));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Thank you for your purchase!", NORMAL_FONT));
    }
}
