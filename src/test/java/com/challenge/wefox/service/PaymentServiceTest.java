package com.challenge.wefox.service;

import com.challenge.wefox.entities.db.Account;
import com.challenge.wefox.entities.db.Payment;
import com.challenge.wefox.exception.AccountException;
import com.challenge.wefox.exception.PaymentException;
import com.challenge.wefox.infrastructure.model.PaymentEvent;
import com.challenge.wefox.repository.AccountRepository;
import com.challenge.wefox.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private final static String INVALID_PAYMENT = "Payment is Invalid";
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private WebClient webClient;

    private PaymentService paymentService;

    @BeforeEach
    void init() {
        paymentService = new PaymentService(paymentRepository, accountRepository);
        webClient = WebClient.create("http://localhost:9000");
    }

    @Test
    void testProcessPayment_ValidPayment() {
        PaymentEvent paymentEvent = createPaymentEvent();
        Account account = createAccount();

        when(accountRepository.findByAccountId(anyLong())).thenReturn(Optional.of(account));

        paymentService.processPayment(paymentEvent);

        verify(accountRepository, times(1)).findByAccountId(anyLong());
    }

    @Test
    void testProcessPayment_AcocuntNotFoundException() {
        PaymentEvent paymentEvent = createPaymentEvent();

        when(accountRepository.findByAccountId(anyLong())).thenReturn(Optional.empty());

        assertThrows(AccountException.class, () -> paymentService.processPayment(paymentEvent),
                "AccountException should be thrown when account is not found");
    }

    @Test
    void testProcessPayment_AccountSaveException(){
        PaymentEvent paymentEvent = createPaymentEvent();
        Account account = createAccount();

        when(accountRepository.findByAccountId(anyLong())).thenReturn(Optional.of(account));
        doThrow(AccountException.class).when(accountRepository).save(account);

        assertThrows(AccountException.class, () -> paymentService.processPayment(paymentEvent),
                "AccountException should be thrown when try to save an account");
    }

    @Test
    void testProcessPayment_PaymentsSaveException(){
        PaymentEvent paymentEvent = createPaymentEvent();
        Account account = createAccount();

        when(accountRepository.findByAccountId(anyLong())).thenReturn(Optional.of(account));
        doThrow(PaymentException.class).when(paymentRepository).save(any(Payment.class));

        assertThrows(PaymentException.class, () -> paymentService.processPayment(paymentEvent),
                "PaymentException should be thrown when try to save a payment");
    }

    private PaymentEvent createPaymentEvent(){
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setPaymentId("123456");
        paymentEvent.setAccountId(1);
        paymentEvent.setPaymentType("online");
        paymentEvent.setCreditCard("12345");
        paymentEvent.setAmount(100);
        return paymentEvent;
    }

    private Payment createPayment(){
        Payment payment = new Payment();
        payment.setPaymentId("123456");
        payment.setAccount(createAccount());
        payment.setPaymentType("online");
        payment.setCreditCard("12345");
        payment.setAmount(BigDecimal.TEN);
        return payment;
    }

    private Account createAccount(){
        Account acc = new Account();
        acc.setAccountId(1l);
        acc.setCreatedOn(LocalDate.now());
        acc.setBirthdate(LocalDate.now());
        acc.setName("name");
        acc.setEmail("email@email.com");
        return acc;
    }

    private WebClient getWebClientMockInvalidPayment() {
        WebClient mockWebClient = Mockito.mock(WebClient.class);

        when(webClient.post().retrieve().bodyToMono(String.class)).thenReturn(Mono.just(INVALID_PAYMENT));

        return mockWebClient;
    }
}