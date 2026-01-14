package com.ecommerce.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("timestamp")
    private Long timestamp;
}
