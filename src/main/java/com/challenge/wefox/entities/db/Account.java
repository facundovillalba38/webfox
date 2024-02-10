package com.challenge.wefox.entities.db;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "birthdate")
    private Date birthdate;

    @Column(name = "last_payment_date")
    private Date lastPaymentDate;

    @Column(name = "created_on")
    private Date createdOn;
}

