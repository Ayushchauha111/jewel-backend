package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LayawayService {
    @Autowired
    private LayawayRepository layawayRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Create layaway: reserve items (set stock RESERVED), record total and initial payment.
     */
    public Layaway createLayaway(Layaway layaway) {
        if (layaway.getCustomer() == null || layaway.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer is required");
        }
        Customer cust = customerRepository.findById(layaway.getCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        layaway.setCustomer(cust);

        BigDecimal total = BigDecimal.ZERO;
        if (layaway.getItems() != null) {
            for (LayawayItem li : layaway.getItems()) {
                li.setLayaway(layaway);
                Stock s = stockRepository.findById(li.getStock().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + li.getStock().getId()));
                if (s.getStatus() != Stock.StockStatus.AVAILABLE) {
                    throw new IllegalArgumentException("Stock " + s.getArticleCode() + " is not available for layaway");
                }
                int qty = li.getQuantity() != null ? li.getQuantity() : 1;
                if (s.getQuantity() == null || s.getQuantity() < qty) {
                    throw new IllegalArgumentException("Insufficient quantity for " + s.getArticleCode());
                }
                s.setStatus(Stock.StockStatus.RESERVED);
                stockRepository.save(s);
                if (li.getTotalPrice() != null) total = total.add(li.getTotalPrice());
            }
        }
        layaway.setTotalAmount(layaway.getTotalAmount() != null ? layaway.getTotalAmount() : total);
        layaway.setPaidAmount(layaway.getPaidAmount() != null ? layaway.getPaidAmount() : BigDecimal.ZERO);
        return layawayRepository.save(layaway);
    }

    /**
     * Add a part payment to layaway.
     */
    public Layaway addPayment(Long layawayId, BigDecimal amount, String notes) {
        Layaway layaway = layawayRepository.findById(layawayId)
                .orElseThrow(() -> new IllegalArgumentException("Layaway not found"));
        if (layaway.getStatus() != Layaway.LayawayStatus.RESERVED) {
            throw new IllegalStateException("Layaway is not in RESERVED status");
        }
        LayawayPayment payment = new LayawayPayment();
        payment.setLayaway(layaway);
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setNotes(notes);
        layaway.getPayments().add(payment);
        BigDecimal newPaid = layaway.getPaidAmount().add(amount);
        layaway.setPaidAmount(newPaid);
        if (newPaid.compareTo(layaway.getTotalAmount()) >= 0) {
            layaway.setStatus(Layaway.LayawayStatus.COMPLETED);
            for (LayawayItem li : layaway.getItems()) {
                Stock s = stockRepository.findById(li.getStock().getId()).orElse(null);
                if (s != null) {
                    s.setStatus(Stock.StockStatus.SOLD);
                    int qty = li.getQuantity() != null ? li.getQuantity() : 1;
                    s.setQuantity((s.getQuantity() != null ? s.getQuantity() : 1) - qty);
                    if (s.getQuantity() <= 0) s.setQuantity(0);
                    stockRepository.save(s);
                }
            }
        }
        return layawayRepository.save(layaway);
    }

    public Optional<Layaway> getById(Long id) {
        return layawayRepository.findById(id);
    }

    public List<Layaway> getByCustomerId(Long customerId) {
        return layawayRepository.findByCustomerId(customerId);
    }

    public List<Layaway> getByStatus(Layaway.LayawayStatus status) {
        return layawayRepository.findByStatus(status);
    }

    public List<Layaway> getAll() {
        return layawayRepository.findAll();
    }

    public Layaway cancel(Long layawayId) {
        Layaway layaway = layawayRepository.findById(layawayId)
                .orElseThrow(() -> new IllegalArgumentException("Layaway not found"));
        if (layaway.getStatus() != Layaway.LayawayStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED layaways can be cancelled");
        }
        for (LayawayItem li : layaway.getItems()) {
            Stock s = stockRepository.findById(li.getStock().getId()).orElse(null);
            if (s != null) {
                s.setStatus(Stock.StockStatus.AVAILABLE);
                stockRepository.save(s);
            }
        }
        layaway.setStatus(Layaway.LayawayStatus.CANCELLED);
        return layawayRepository.save(layaway);
    }
}
