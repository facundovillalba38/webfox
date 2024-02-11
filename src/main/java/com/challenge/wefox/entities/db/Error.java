package com.challenge.wefox.entities.db;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "errors")
@Getter
@Setter
public class Error {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long errorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "error", length = 20)
    private com.challenge.wefox.entities.ErrorType errorType;

    @Column(name = "error_description")
    private String errorDescription;
}
