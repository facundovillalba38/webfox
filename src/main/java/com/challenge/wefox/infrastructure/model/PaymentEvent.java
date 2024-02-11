package com.challenge.wefox.infrastructure.model;

import com.challenge.wefox.entities.db.Account;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
public class PaymentEvent implements Serializable {
    private static final long serialVersionUID = -6915528229848073283L;
    @SerializedName("payment_id")
    private String paymentId;
    @SerializedName("account_id")
    private int accountId;
    @SerializedName("payment_type")
    private String paymentType;
    @SerializedName("credit_card")
    private String creditCard;
    private double amount;
}
