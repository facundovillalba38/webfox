package com.challenge.wefox.infrastructure.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
