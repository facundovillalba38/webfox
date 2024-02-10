package com.challenge.wefox.service;

import com.challenge.wefox.entities.db.Payment;
import com.challenge.wefox.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public boolean validatePayment(Payment payment) {
        // Implement payment validation logic here (calling third-party API)
        // For demonstration, assume all payments are valid
        return true;
    }

    public void processPayment(Payment payment) {
        // Implement payment processing logic here (e.g., storing payment in database)
        System.out.println("Processing payment: " + payment);
    }
}
