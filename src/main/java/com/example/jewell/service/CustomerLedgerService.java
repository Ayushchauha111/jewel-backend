package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomerLedgerService {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private CreditRepository creditRepository;

    @Autowired
    private CreditPaymentHistoryRepository creditPaymentHistoryRepository;

    /**
     * Single view per customer: bills, credits (udhari), credit payment history.
     */
    public Map<String, Object> getCustomerLedger(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        List<Billing> bills = billingRepository.findByCustomerId(customerId);
        List<Credit> credits = creditRepository.findByCustomerId(customerId);

        List<Map<String, Object>> billSummaries = bills.stream()
                .sorted(Comparator.comparing(Billing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toBillSummary)
                .collect(Collectors.toList());

        List<Map<String, Object>> creditSummaries = new ArrayList<>();
        for (Credit c : credits) {
            Map<String, Object> cs = toCreditSummary(c);
            List<CreditPaymentHistory> payments = creditPaymentHistoryRepository.findByCreditIdOrderByPaymentDateDesc(c.getId());
            cs.put("payments", payments.stream().map(this::toPaymentSummary).collect(Collectors.toList()));
            creditSummaries.add(cs);
        }
        creditSummaries.sort(Comparator.comparing(m -> (java.time.LocalDateTime) m.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, Object> ledger = new HashMap<>();
        ledger.put("customer", toCustomerSummary(customer));
        ledger.put("bills", billSummaries);
        ledger.put("credits", creditSummaries);
        return ledger;
    }

    private Map<String, Object> toCustomerSummary(Customer c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("phone", c.getPhone());
        m.put("email", c.getEmail());
        m.put("address", c.getAddress());
        m.put("whatsappNumber", c.getWhatsappNumber());
        m.put("loyaltyPoints", c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : java.math.BigDecimal.ZERO);
        return m;
    }

    private Map<String, Object> toBillSummary(Billing b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("billNumber", b.getBillNumber());
        m.put("finalAmount", b.getFinalAmount());
        m.put("paidAmount", b.getPaidAmount());
        m.put("paymentStatus", b.getPaymentStatus() != null ? b.getPaymentStatus().name() : null);
        m.put("paymentMethod", b.getPaymentMethod() != null ? b.getPaymentMethod().name() : null);
        m.put("createdAt", b.getCreatedAt());
        return m;
    }

    private Map<String, Object> toCreditSummary(Credit c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("creditAmount", c.getCreditAmount());
        m.put("paidAmount", c.getPaidAmount());
        m.put("remainingAmount", c.getRemainingAmount());
        m.put("status", c.getStatus() != null ? c.getStatus().name() : null);
        m.put("description", c.getDescription());
        m.put("billingId", c.getBilling() != null ? c.getBilling().getId() : null);
        m.put("billNumber", c.getBilling() != null ? c.getBilling().getBillNumber() : null);
        m.put("createdAt", c.getCreatedAt());
        m.put("updatedAt", c.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toPaymentSummary(CreditPaymentHistory p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("paymentAmount", p.getPaymentAmount());
        m.put("paymentDate", p.getPaymentDate());
        m.put("notes", p.getNotes());
        m.put("createdAt", p.getCreatedAt());
        return m;
    }
}
