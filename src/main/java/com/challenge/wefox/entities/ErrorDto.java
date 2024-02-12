package com.challenge.wefox.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ErrorDto {
    @JsonProperty("payment_id")
    private String paymentId;
    @JsonProperty("error_type")
    private String errorType;
    @JsonProperty("error_description")
    private String errorDescription;
}
