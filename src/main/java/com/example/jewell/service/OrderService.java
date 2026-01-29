package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Page<Order> getAllOrdersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public Order createOrder(Order order) {
        // Generate order number
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber(generateOrderNumber());
        }

        // Set the order reference on each item (required for bidirectional relationship)
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
            }
        }

        // Calculate totals
        BigDecimal totalAmount = order.getItems() != null ? order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        order.setTotalAmount(totalAmount);
        BigDecimal finalAmount = totalAmount.add(
            order.getShippingCharge() != null ? order.getShippingCharge() : new BigDecimal("10.00")
        );
        order.setFinalAmount(finalAmount);

        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setOrderStatus(status);
        return orderRepository.save(order);
    }

    public Order updatePaymentStatus(Long id, Order.PaymentStatus status, String paymentId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setPaymentStatus(status);
        if (paymentId != null) {
            order.setPaymentId(paymentId);
        }
        Order savedOrder = orderRepository.save(order);
        
        // Record income transaction if order is paid
        if (status == Order.PaymentStatus.PAID) {
            incomeExpenseService.recordIncomeFromOrder(savedOrder);
        }
        
        return savedOrder;
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = orderRepository.count() + 1;
        return String.format("%s-%s-%05d", prefix, date, count);
    }
}
