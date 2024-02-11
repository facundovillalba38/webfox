package com.challenge.wefox.entities;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class ErrorDto implements Serializable {
    private static final long serialVersionUID = -6915528229848073283L;
    @SerializedName("payment_id")
    private String paymentId;
    @SerializedName("error_type")
    private com.challenge.wefox.entities.ErrorType errorType;
    @SerializedName("error_description")
    private String errorDescription;
}
