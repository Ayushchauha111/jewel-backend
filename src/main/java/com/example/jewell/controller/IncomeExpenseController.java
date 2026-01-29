package com.example.jewell.controller;

import com.example.jewell.model.TransactionHistory;
import com.example.jewell.service.IncomeExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/income-expense")
public class IncomeExpenseController {
    @Autowired
    private IncomeExpenseService incomeExpenseService;

    @PostMapping("/income")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionHistory> recordIncome(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam BigDecimal amount,
            @RequestParam TransactionHistory.Category category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) TransactionHistory.PaymentMethod paymentMethod,
            @RequestParam(required = false) String referenceNumber) {
        TransactionHistory transaction = incomeExpenseService.recordIncome(
                date, amount, category,
                description != null ? description : "",
                paymentMethod != null ? paymentMethod : TransactionHistory.PaymentMethod.CASH,
                referenceNumber);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/expense")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionHistory> recordExpense(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam BigDecimal amount,
            @RequestParam TransactionHistory.Category category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) TransactionHistory.PaymentMethod paymentMethod,
            @RequestParam(required = false) String referenceNumber) {
        TransactionHistory transaction = incomeExpenseService.recordExpense(
                date, amount, category,
                description != null ? description : "",
                paymentMethod != null ? paymentMethod : TransactionHistory.PaymentMethod.CASH,
                referenceNumber);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/income-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getIncomeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(incomeExpenseService.getIncomeSummary(startDate, endDate));
    }

    @GetMapping("/expense-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExpenseSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(incomeExpenseService.getExpenseSummary(startDate, endDate));
    }

    @GetMapping("/financial-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFinancialSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(incomeExpenseService.getFinancialSummary(startDate, endDate));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransactionHistory>> getTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(incomeExpenseService.getTransactions(startDate, endDate));
    }

    @GetMapping("/transactions/type")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransactionHistory>> getTransactionsByType(
            @RequestParam TransactionHistory.TransactionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(incomeExpenseService.getTransactionsByType(type, startDate, endDate));
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        incomeExpenseService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
