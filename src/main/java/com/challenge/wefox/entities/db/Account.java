package com.challenge.wefox.entities.db;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "created_on")
    private LocalDate createdOn;
}

