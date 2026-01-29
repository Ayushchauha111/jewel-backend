package com.example.jewell.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.jewell.service.EmailService;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send")
    public String sendMail(@RequestParam String to, 
                           @RequestParam String subject, 
                           @RequestParam String body) {
        emailService.sendEmail(to, subject, body);
        return "Email sent to " + to;
    }
}