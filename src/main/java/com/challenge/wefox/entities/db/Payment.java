package com.challenge.wefox.entities.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @ManyToOne
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", nullable = false)
    private Account account;

    @Column(name = "payment_type", length = 150, nullable = false)
    private String paymentType;

    @Column(name = "credit_card", length = 100)
    private String creditCard;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "created_on")
    private LocalDate createdOn;
}

