package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalyticsService {
    @Autowired
    private BillingRepository billingRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CreditRepository creditRepository;
    
    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    /**
     * Get daily analytics for a specific date
     */
    public Map<String, Object> getDailyAnalytics(LocalDate date) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Sales Analytics
        BigDecimal totalSales = billingRepository.getTotalSalesByDate(date);
        Long billCount = billingRepository.getBillCountByDate(date);
        
        // Online Sales Analytics
        BigDecimal totalOnlineSales = orderRepository.getTotalOnlineSalesByDate(date);
        Long orderCount = orderRepository.getOrderCountByDate(date);
        
        // Credit Analytics
        BigDecimal totalCreditGiven = creditRepository.getTotalCreditGivenByDate(date);
        BigDecimal totalCreditPaid = creditRepository.getTotalCreditPaidByDate(date);
        
        // Stock Analytics
        Long soldStockCount = stockRepository.getSoldStockCountByDate(date);
        List<Stock> soldStocks = stockRepository.getSoldStockByDate(date);
        
        // Total Income
        BigDecimal totalIncome = (totalSales != null ? totalSales : BigDecimal.ZERO)
                .add(totalOnlineSales != null ? totalOnlineSales : BigDecimal.ZERO)
                .add(totalCreditPaid != null ? totalCreditPaid : BigDecimal.ZERO);
        
        // Expenses (from transaction history)
        BigDecimal totalExpenses = transactionHistoryRepository.getTotalByDateAndType(date, TransactionHistory.TransactionType.EXPENSE);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        
        // Net Income
        BigDecimal netIncome = totalIncome.subtract(totalExpenses);
        
        analytics.put("date", date.toString());
        analytics.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
        analytics.put("billCount", billCount != null ? billCount : 0);
        analytics.put("totalOnlineSales", totalOnlineSales != null ? totalOnlineSales : BigDecimal.ZERO);
        analytics.put("orderCount", orderCount != null ? orderCount : 0);
        analytics.put("totalCreditGiven", totalCreditGiven != null ? totalCreditGiven : BigDecimal.ZERO);
        analytics.put("totalCreditPaid", totalCreditPaid != null ? totalCreditPaid : BigDecimal.ZERO);
        analytics.put("soldStockCount", soldStockCount != null ? soldStockCount : 0);
        analytics.put("soldStocks", soldStocks.stream().map(this::mapStockToSummary).collect(Collectors.toList()));
        analytics.put("totalIncome", totalIncome);
        analytics.put("totalExpenses", totalExpenses);
        analytics.put("netIncome", netIncome);
        
        return analytics;
    }
    
    /**
     * Get analytics for a date range
     */
    public Map<String, Object> getAnalyticsByDateRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Get all bills in date range
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<Billing> bills = billingRepository.findByCreatedAtBetween(start, end);
        
        // Calculate totals
        BigDecimal totalSales = bills.stream()
                .filter(b -> b.getPaymentStatus() == Billing.PaymentStatus.PAID)
                .map(Billing::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Get transactions
        BigDecimal totalIncome = transactionHistoryRepository.getTotalByDateRangeAndType(
                startDate, endDate, TransactionHistory.TransactionType.INCOME);
        BigDecimal totalExpenses = transactionHistoryRepository.getTotalByDateRangeAndType(
                startDate, endDate, TransactionHistory.TransactionType.EXPENSE);
        
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        
        analytics.put("startDate", startDate.toString());
        analytics.put("endDate", endDate.toString());
        analytics.put("totalSales", totalSales);
        analytics.put("billCount", bills.size());
        analytics.put("totalIncome", totalIncome);
        analytics.put("totalExpenses", totalExpenses);
        analytics.put("netIncome", totalIncome.subtract(totalExpenses));
        
        return analytics;
    }

    /**
     * Sales by category for date range (from billing items via stock category).
     */
    public List<Map<String, Object>> getSalesByCategory(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<Billing> bills = billingRepository.findByCreatedAtBetween(start, end);
        Map<String, BigDecimal> categoryToTotal = new HashMap<>();
        for (Billing b : bills) {
            if (b.getPaymentStatus() != Billing.PaymentStatus.PAID && b.getPaymentStatus() != Billing.PaymentStatus.PARTIAL) continue;
            if (b.getItems() == null) continue;
            for (BillingItem bi : b.getItems()) {
                String category = "Other";
                if (bi.getStock() != null && bi.getStock().getCategory() != null && !bi.getStock().getCategory().isEmpty()) {
                    category = bi.getStock().getCategory();
                }
                BigDecimal amt = bi.getTotalPrice() != null ? bi.getTotalPrice() : BigDecimal.ZERO;
                categoryToTotal.merge(category, amt, BigDecimal::add);
            }
        }
        return categoryToTotal.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("category", e.getKey());
                    m.put("totalSales", e.getValue());
                    return m;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("category")))
                .collect(Collectors.toList());
    }

    /**
     * Sales by payment method for date range.
     */
    public List<Map<String, Object>> getSalesByPaymentMethod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<Billing> bills = billingRepository.findByCreatedAtBetween(start, end);
        Map<Billing.PaymentMethod, BigDecimal> methodToTotal = new HashMap<>();
        for (Billing b : bills) {
            if (b.getPaymentStatus() != Billing.PaymentStatus.PAID && b.getPaymentStatus() != Billing.PaymentStatus.PARTIAL) continue;
            Billing.PaymentMethod method = b.getPaymentMethod() != null ? b.getPaymentMethod() : Billing.PaymentMethod.CASH;
            BigDecimal amt = b.getPaidAmount() != null ? b.getPaidAmount() : b.getFinalAmount();
            if (amt == null) amt = BigDecimal.ZERO;
            methodToTotal.merge(method, amt, BigDecimal::add);
        }
        return methodToTotal.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("paymentMethod", e.getKey().name());
                    m.put("totalAmount", e.getValue());
                    return m;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("paymentMethod")))
                .collect(Collectors.toList());
    }

    /**
     * GSTR-style tax summary for date range: taxable value, CGST, SGST, total GST.
     */
    public Map<String, Object> getTaxSummary(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<Billing> bills = billingRepository.findByCreatedAtBetween(start, end);
        BigDecimal taxableValue = bills.stream()
                .filter(b -> b.getPaymentStatus() == Billing.PaymentStatus.PAID || b.getPaymentStatus() == Billing.PaymentStatus.PARTIAL)
                .map(Billing::getFinalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgstRate = new BigDecimal("0.015");
        BigDecimal sgstRate = new BigDecimal("0.015");
        BigDecimal cgst = taxableValue.multiply(cgstRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = taxableValue.multiply(sgstRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGst = cgst.add(sgst);
        Map<String, Object> out = new HashMap<>();
        out.put("startDate", startDate.toString());
        out.put("endDate", endDate.toString());
        out.put("taxableValue", taxableValue);
        out.put("cgstRate", "1.5%");
        out.put("sgstRate", "1.5%");
        out.put("cgst", cgst);
        out.put("sgst", sgst);
        out.put("totalGst", totalGst);
        out.put("totalWithGst", taxableValue.add(totalGst));
        return out;
    }

    /**
     * Get completed stock items for a specific date
     */
    public List<Map<String, Object>> getCompletedStockByDate(LocalDate date) {
        List<Stock> soldStocks = stockRepository.getSoldStockByDate(date);
        return soldStocks.stream()
                .map(this::mapStockToDetail)
                .collect(Collectors.toList());
    }

    /**
     * Get credit history for a date range
     */
    public List<Map<String, Object>> getCreditHistory(LocalDate startDate, LocalDate endDate) {
        List<Credit> credits = creditRepository.findAll().stream()
                .filter(c -> {
                    LocalDate creditDate = c.getCreatedAt().toLocalDate();
                    return !creditDate.isBefore(startDate) && !creditDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        
        return credits.stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("customerName", c.getCustomer().getName());
                    map.put("creditAmount", c.getCreditAmount());
                    map.put("paidAmount", c.getPaidAmount());
                    map.put("remainingAmount", c.getRemainingAmount());
                    map.put("status", c.getStatus().toString());
                    map.put("description", c.getDescription());
                    map.put("createdAt", c.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapStockToSummary(Stock stock) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", stock.getId());
        map.put("articleName", stock.getArticleName());
        map.put("articleCode", stock.getArticleCode());
        map.put("sellingPrice", stock.getSellingPrice());
        map.put("soldDate", stock.getUpdatedAt());
        return map;
    }

    private Map<String, Object> mapStockToDetail(Stock stock) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", stock.getId());
        map.put("articleName", stock.getArticleName());
        map.put("articleCode", stock.getArticleCode());
        map.put("weightGrams", stock.getWeightGrams());
        map.put("carat", stock.getCarat());
        map.put("purityPercentage", stock.getPurityPercentage());
        map.put("purchasePrice", stock.getPurchasePrice());
        map.put("sellingPrice", stock.getSellingPrice());
        map.put("quantity", stock.getQuantity());
        map.put("status", stock.getStatus().toString());
        map.put("soldDate", stock.getUpdatedAt());
        return map;
    }
}
