package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class IncomeExpenseService {
    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    /**
     * Record an income transaction
     */
    public TransactionHistory recordIncome(LocalDate date, BigDecimal amount, 
                                           TransactionHistory.Category category,
                                           String description, TransactionHistory.PaymentMethod paymentMethod,
                                           String referenceNumber) {
        TransactionHistory transaction = new TransactionHistory();
        transaction.setTransactionDate(date);
        transaction.setTransactionType(TransactionHistory.TransactionType.INCOME);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setReferenceNumber(referenceNumber);
        return transactionHistoryRepository.save(transaction);
    }

    /**
     * Record an expense transaction
     */
    public TransactionHistory recordExpense(LocalDate date, BigDecimal amount,
                                           TransactionHistory.Category category,
                                           String description, TransactionHistory.PaymentMethod paymentMethod,
                                           String referenceNumber) {
        TransactionHistory transaction = new TransactionHistory();
        transaction.setTransactionDate(date);
        transaction.setTransactionType(TransactionHistory.TransactionType.EXPENSE);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setReferenceNumber(referenceNumber);
        return transactionHistoryRepository.save(transaction);
    }

    /**
     * Automatically record income from a billing (when bill is paid or partially paid)
     */
    public void recordIncomeFromBilling(Billing billing) {
        if (billing.getPaymentStatus() == Billing.PaymentStatus.PAID || 
            billing.getPaymentStatus() == Billing.PaymentStatus.PARTIAL) {
            TransactionHistory transaction = new TransactionHistory();
            transaction.setTransactionDate(billing.getCreatedAt().toLocalDate());
            transaction.setTransactionType(TransactionHistory.TransactionType.INCOME);
            transaction.setCategory(TransactionHistory.Category.SALES);
            
            // Use paid amount if available, otherwise use final amount
            BigDecimal amountToRecord = billing.getPaidAmount() != null && 
                                       billing.getPaidAmount().compareTo(BigDecimal.ZERO) > 0
                                       ? billing.getPaidAmount() 
                                       : billing.getFinalAmount();
            transaction.setAmount(amountToRecord);
            
            String description = "Bill: " + billing.getBillNumber();
            if (billing.getPaymentStatus() == Billing.PaymentStatus.PARTIAL && 
                billing.getPaidAmount() != null) {
                description += " (Partial payment: ₹" + billing.getPaidAmount() + ")";
            }
            transaction.setDescription(description);
            
            // Map Billing.PaymentMethod to TransactionHistory.PaymentMethod
            TransactionHistory.PaymentMethod paymentMethod = TransactionHistory.PaymentMethod.CASH;
            if (billing.getPaymentMethod() != null) {
                try {
                    String methodName = billing.getPaymentMethod().name();
                    paymentMethod = TransactionHistory.PaymentMethod.valueOf(methodName);
                } catch (IllegalArgumentException e) {
                    // If enum value doesn't exist, default to CASH
                    paymentMethod = TransactionHistory.PaymentMethod.CASH;
                }
            }
            transaction.setPaymentMethod(paymentMethod);
            transaction.setReferenceNumber(billing.getBillNumber());
            transaction.setBilling(billing);
            transactionHistoryRepository.save(transaction);
        }
    }

    /**
     * Automatically record income from an order (when order is paid)
     */
    public void recordIncomeFromOrder(Order order) {
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            TransactionHistory transaction = new TransactionHistory();
            transaction.setTransactionDate(order.getCreatedAt().toLocalDate());
            transaction.setTransactionType(TransactionHistory.TransactionType.INCOME);
            transaction.setCategory(TransactionHistory.Category.ONLINE_SALES);
            transaction.setAmount(order.getFinalAmount());
            transaction.setDescription("Online Order: " + order.getOrderNumber());
            transaction.setPaymentMethod(TransactionHistory.PaymentMethod.ONLINE);
            transaction.setReferenceNumber(order.getOrderNumber());
            transaction.setOrder(order);
            transactionHistoryRepository.save(transaction);
        }
    }

    /**
     * Record income from credit payment
     */
    public void recordIncomeFromCreditPayment(Credit credit, BigDecimal paymentAmount) {
        TransactionHistory transaction = new TransactionHistory();
        transaction.setTransactionDate(LocalDate.now());
        transaction.setTransactionType(TransactionHistory.TransactionType.INCOME);
        transaction.setCategory(TransactionHistory.Category.CREDIT_PAYMENT);
        transaction.setAmount(paymentAmount);
        transaction.setDescription("Credit Payment: " + credit.getDescription());
        transaction.setPaymentMethod(TransactionHistory.PaymentMethod.CASH);
        transaction.setReferenceNumber("CREDIT-" + credit.getId());
        transaction.setCredit(credit);
        transactionHistoryRepository.save(transaction);
    }

    /**
     * Record expense for stock purchase
     * Note: Purchase price is now optional, so this method only records if purchase price is provided
     */
    public void recordStockPurchaseExpense(Stock stock) {
        // Only record if purchase price is provided
        // Since purchase price is now removed from the UI, this will typically not be called
        // But keeping it for backward compatibility
        if (stock.getPurchasePrice() != null && stock.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            TransactionHistory transaction = new TransactionHistory();
            transaction.setTransactionDate(stock.getCreatedAt().toLocalDate());
            transaction.setTransactionType(TransactionHistory.TransactionType.EXPENSE);
            transaction.setCategory(TransactionHistory.Category.STOCK_PURCHASE);
            transaction.setAmount(stock.getPurchasePrice());
            transaction.setDescription("Stock Purchase: " + stock.getArticleName() + 
                (stock.getArticleCode() != null ? " (" + stock.getArticleCode() + ")" : ""));
            transaction.setPaymentMethod(TransactionHistory.PaymentMethod.CASH);
            transaction.setReferenceNumber(stock.getArticleCode() != null ? stock.getArticleCode() : "STOCK-" + stock.getId());
            transaction.setStock(stock);
            transactionHistoryRepository.save(transaction);
        }
    }

    /**
     * Get income summary for a date range (for tax purposes)
     */
    public Map<String, Object> getIncomeSummary(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new HashMap<>();
        
        List<TransactionHistory> incomeTransactions = transactionHistoryRepository
                .findByTransactionDateBetween(startDate, endDate).stream()
                .filter(t -> t.getTransactionType() == TransactionHistory.TransactionType.INCOME)
                .collect(Collectors.toList());
        
        BigDecimal totalIncome = incomeTransactions.stream()
                .map(TransactionHistory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Group by category
        Map<TransactionHistory.Category, BigDecimal> incomeByCategory = incomeTransactions.stream()
                .collect(Collectors.groupingBy(
                    TransactionHistory::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, TransactionHistory::getAmount, BigDecimal::add)
                ));
        
        // Group by payment method
        Map<TransactionHistory.PaymentMethod, BigDecimal> incomeByPaymentMethod = incomeTransactions.stream()
                .filter(t -> t.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(
                    TransactionHistory::getPaymentMethod,
                    Collectors.reducing(BigDecimal.ZERO, TransactionHistory::getAmount, BigDecimal::add)
                ));
        
        summary.put("startDate", startDate.toString());
        summary.put("endDate", endDate.toString());
        summary.put("totalIncome", totalIncome);
        summary.put("incomeByCategory", incomeByCategory);
        summary.put("incomeByPaymentMethod", incomeByPaymentMethod);
        summary.put("transactionCount", incomeTransactions.size());
        // Do not include full transactions list to avoid lazy-load/serialization issues

        return summary;
    }

    /**
     * Get expense summary for a date range (for tax purposes)
     */
    public Map<String, Object> getExpenseSummary(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new HashMap<>();
        
        List<TransactionHistory> expenseTransactions = transactionHistoryRepository
                .findByTransactionDateBetween(startDate, endDate).stream()
                .filter(t -> t.getTransactionType() == TransactionHistory.TransactionType.EXPENSE)
                .collect(Collectors.toList());
        
        BigDecimal totalExpenses = expenseTransactions.stream()
                .map(TransactionHistory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Group by category
        Map<TransactionHistory.Category, BigDecimal> expensesByCategory = expenseTransactions.stream()
                .collect(Collectors.groupingBy(
                    TransactionHistory::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, TransactionHistory::getAmount, BigDecimal::add)
                ));
        
        summary.put("startDate", startDate.toString());
        summary.put("endDate", endDate.toString());
        summary.put("totalExpenses", totalExpenses);
        summary.put("expensesByCategory", expensesByCategory);
        summary.put("transactionCount", expenseTransactions.size());
        // Do not include full transactions list to avoid lazy-load/serialization issues

        return summary;
    }

    /**
     * Get complete financial summary (income, expenses, net) for tax purposes
     */
    public Map<String, Object> getFinancialSummary(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new HashMap<>();
        
        Map<String, Object> incomeSummary = getIncomeSummary(startDate, endDate);
        Map<String, Object> expenseSummary = getExpenseSummary(startDate, endDate);
        
        BigDecimal totalIncome = (BigDecimal) incomeSummary.get("totalIncome");
        BigDecimal totalExpenses = (BigDecimal) expenseSummary.get("totalExpenses");
        BigDecimal netIncome = totalIncome.subtract(totalExpenses);
        
        summary.put("startDate", startDate.toString());
        summary.put("endDate", endDate.toString());
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpenses);
        summary.put("netIncome", netIncome);
        summary.put("incomeDetails", incomeSummary);
        summary.put("expenseDetails", expenseSummary);
        
        return summary;
    }

    /**
     * Get all transactions for a date range
     */
    public List<TransactionHistory> getTransactions(LocalDate startDate, LocalDate endDate) {
        return transactionHistoryRepository.findByTransactionDateBetween(startDate, endDate);
    }

    /**
     * Get transactions by type
     */
    public List<TransactionHistory> getTransactionsByType(TransactionHistory.TransactionType type, 
                                                          LocalDate startDate, LocalDate endDate) {
        return transactionHistoryRepository.findByTransactionDateBetween(startDate, endDate).stream()
                .filter(t -> t.getTransactionType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Delete a transaction
     */
    public void deleteTransaction(Long id) {
        transactionHistoryRepository.deleteById(id);
    }
}
