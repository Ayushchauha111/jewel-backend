package com.example.jewell.controller;

import com.example.jewell.model.Customer;
import com.example.jewell.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCustomers(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Customer> customerPage = customerService.getAllCustomersPaginated(page, size);
        return ResponseEntity.ok(com.example.jewell.dto.PageResponse.of(customerPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(@RequestParam String name) {
        return ResponseEntity.ok(customerService.searchCustomersByName(name));
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<Customer> getCustomerByPhone(@PathVariable String phone) {
        return customerService.getCustomerByPhone(phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    // Allow public access for checkout flow - security is handled in WebSecurityConfig
    public ResponseEntity<?> createCustomer(@RequestBody Customer customer) {
        try {
            // Log incoming request for debugging
            System.out.println("Creating customer - Name: " + customer.getName() + ", Phone: " + customer.getPhone() + ", Email: " + customer.getEmail());
            
            // Validate required fields
            if (customer.getName() == null || customer.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }
            if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone is required"));
            }
            
            // Check if customer with same phone already exists
            Optional<Customer> existingCustomer = customerService.getCustomerByPhone(customer.getPhone().trim());
            if (existingCustomer.isPresent()) {
                System.out.println("Customer already exists with phone: " + customer.getPhone());
                return ResponseEntity.ok(existingCustomer.get());
            }
            
            Customer savedCustomer = customerService.createCustomer(customer);
            System.out.println("Customer created successfully with ID: " + savedCustomer.getId());
            return ResponseEntity.ok(savedCustomer);
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error creating customer: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            System.err.println("Error creating customer: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create customer");
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " - " + e.getCause().getMessage();
            }
            error.put("message", errorMsg != null ? errorMsg : "Unknown error occurred");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            Map<String, String> body = new HashMap<>();
            body.put("error", "Cannot delete customer");
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
}
