package com.example.jewell.repository;

import com.example.jewell.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    // Pagination methods
    Page<Customer> findAll(Pageable pageable);
    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
