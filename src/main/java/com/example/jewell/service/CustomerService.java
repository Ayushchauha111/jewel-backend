package com.example.jewell.service;

import com.example.jewell.model.Customer;
import com.example.jewell.repository.BillingRepository;
import com.example.jewell.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillingRepository billingRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Page<Customer> getAllCustomersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return customerRepository.findAll(pageable);
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> getCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    public List<Customer> searchCustomersByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name);
    }

    public Customer createCustomer(Customer customer) {
        // Trim whitespace from string fields
        if (customer.getName() != null) {
            customer.setName(customer.getName().trim());
        }
        if (customer.getPhone() != null) {
            customer.setPhone(customer.getPhone().trim());
        }
        if (customer.getEmail() != null) {
            customer.setEmail(customer.getEmail().trim());
        }
        if (customer.getAddress() != null) {
            customer.setAddress(customer.getAddress().trim());
        }
        if (customer.getWhatsappNumber() != null) {
            customer.setWhatsappNumber(customer.getWhatsappNumber().trim());
        }
        
        // Validate email format if provided (allow empty)
        if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
            String email = customer.getEmail().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Invalid email format: " + email);
            }
        } else {
            // Set to null if empty
            customer.setEmail(null);
        }
        
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        
        customer.setName(customerDetails.getName());
        customer.setPhone(customerDetails.getPhone());
        customer.setEmail(customerDetails.getEmail());
        customer.setAddress(customerDetails.getAddress());
        customer.setWhatsappNumber(customerDetails.getWhatsappNumber());
        
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        long billCount = billingRepository.countByCustomerId(id);
        if (billCount > 0) {
            throw new IllegalStateException("Cannot delete customer: they have " + billCount + " existing bill(s). Remove or reassign bills first.");
        }
        customerRepository.deleteById(id);
    }
}
