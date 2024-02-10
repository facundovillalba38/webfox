package com.challenge.wefox.controller;

import com.challenge.wefox.entities.db.Payment;
import com.challenge.wefox.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("")
    public ResponseEntity<String> processPayment(@RequestBody Payment payment) {
        boolean isValid = paymentService.validatePayment(payment);
        if (isValid) {
            return new ResponseEntity<>("Payment processed successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid payment", HttpStatus.BAD_REQUEST);
        }
    }
}
