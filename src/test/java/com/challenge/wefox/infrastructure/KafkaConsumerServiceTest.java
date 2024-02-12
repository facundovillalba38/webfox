package com.challenge.wefox.infrastructure;

import com.challenge.wefox.infrastructure.model.PaymentEvent;
import com.challenge.wefox.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {
    @Mock
    private PaymentService paymentService;

    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void init(){
        kafkaConsumerService = new KafkaConsumerService(paymentService);
    }

    @Test
    public void testConsumePayments() {
        String paymentJson = "{\"payment_id\":\"05359452-594f-46bd-8fcf-6491c4b37687\",\"account_id\":834,\"payment_type\":\"offline\",\"credit_card\":\"\",\"amount\":49,\"delay\":493}";
        GenericMessage<String> paymentMessage = new GenericMessage<>(paymentJson);

        kafkaConsumerService.consumePayments(paymentMessage);

        verify(paymentService, times(1)).processPayment(any(PaymentEvent.class));
    }
}