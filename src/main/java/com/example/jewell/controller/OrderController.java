package com.example.jewell.controller;

import com.example.jewell.model.Order;
import com.example.jewell.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Order> orderPage = orderService.getAllOrdersPaginated(page, size);
        return ResponseEntity.ok(com.example.jewell.dto.PageResponse.of(orderPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<Order> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody PaymentStatusUpdate request) {
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, request.getStatus(), request.getPaymentId()));
    }

    public static class PaymentStatusUpdate {
        private Order.PaymentStatus status;
        private String paymentId;

        public Order.PaymentStatus getStatus() {
            return status;
        }

        public void setStatus(Order.PaymentStatus status) {
            this.status = status;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }
    }
}
