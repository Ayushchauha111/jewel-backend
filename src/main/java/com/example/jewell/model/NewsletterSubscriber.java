package com.example.jewell.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscribers", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email")
})
public class NewsletterSubscriber {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private boolean subscribed = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime subscribedAt;
    
    private LocalDateTime unsubscribedAt;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isSubscribed() {
        return subscribed;
    }
    
    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
        if (!subscribed) {
            this.unsubscribedAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }
    
    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
    
    public LocalDateTime getUnsubscribedAt() {
        return unsubscribedAt;
    }
    
    public void setUnsubscribedAt(LocalDateTime unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }
}


