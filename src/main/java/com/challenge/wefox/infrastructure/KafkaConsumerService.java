package com.challenge.wefox.infrastructure;

import com.challenge.wefox.entities.db.Payment;
import com.challenge.wefox.service.PaymentService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumerService {
    private final PaymentService paymentService;

    public KafkaConsumerService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = {"online", "offline"},
            groupId = "payment-processor",
            properties = {
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG + "=localhost:29092",
                    ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS+ "=org.springframework.kafka.support.serializer.JsonDeserializer",
                    ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS+ "=org.apache.kafka.common.serialization.StringDeserializer",
                    JsonDeserializer.VALUE_DEFAULT_TYPE + "=com.challenge.wefox.entities.db.Payment",
                    JsonDeserializer.TRUSTED_PACKAGES + "*",
            })
    public void consumePayments(Payment payment) {
        paymentService.processPayment(payment);
    }
}