package com.challenge.wefox.service;

import com.challenge.wefox.entities.ErrorDto;
import com.challenge.wefox.entities.ErrorType;
import com.challenge.wefox.entities.db.Account;
import com.challenge.wefox.entities.db.Payment;
import com.challenge.wefox.exception.AccountException;
import com.challenge.wefox.exception.PaymentException;
import com.challenge.wefox.infrastructure.model.PaymentEvent;
import com.challenge.wefox.repository.AccountRepository;
import com.challenge.wefox.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service for processing payments and handling related business logic.
 */
@Service
public class PaymentService {
    private static final String VALID_PAYMENT = "Payment is Valid";
    private static final String INVALID_PAYMENT = "Payment is Invalid";
    private static final String PAYMENT_URL_SUFFIX = "/payment";
    private static final String LOG_URL_SUFFIX = "/log";

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final WebClient webClient;
    private final String apiProducerHost;

    /**
     * Constructs a PaymentService with required dependencies.
     * @param paymentRepository the payment repository
     * @param accountRepository the account repository
     * @param webClient the WebClient for external calls
     * @param apiProducerHost the API producer host
     */
    public PaymentService(PaymentRepository paymentRepository, AccountRepository accountRepository, WebClient webClient, @Value("${api.producer.host}") String apiProducerHost) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.webClient = webClient;
        this.apiProducerHost = apiProducerHost;
    }

    /**
     * Processes a payment event: validates, persists, and logs as needed.
     * @param payment the payment event
     */
    public void processPayment(PaymentEvent payment) {
        Mono<String> response = checkPaymentValid(payment);
        processValidPayment(response.block(), payment);
    }

    private void processValidPayment(String response, PaymentEvent paymentEvent) {
        if (VALID_PAYMENT.equals(response)) {
            Account account = getAndSaveAccount(paymentEvent);
            createAndSavePayment(paymentEvent, account);
        } else if (INVALID_PAYMENT.equals(response)) {
            storeErrorLogs(ErrorDto.builder()
                    .paymentId(paymentEvent.getPaymentId())
                    .errorType(ErrorType.NETWORK.name())
                    .errorDescription(INVALID_PAYMENT)
                    .build());
        }
    }

    private void createAndSavePayment(PaymentEvent paymentEvent, Account account) {
        try {
            Payment payment = new Payment();
            payment.setPaymentId(paymentEvent.getPaymentId());
            payment.setPaymentType(paymentEvent.getPaymentType());
            payment.setCreatedOn(LocalDate.now());
            payment.setCreditCard(paymentEvent.getCreditCard());
            payment.setAccount(account);
            payment.setAmount(BigDecimal.valueOf(paymentEvent.getAmount()));
            paymentRepository.save(payment);
        } catch (Exception ex) {
            storeErrorLogs(ErrorDto.builder()
                    .paymentId(paymentEvent.getPaymentId())
                    .errorType(ErrorType.DATABASE.name())
                    .errorDescription(ex.getMessage())
                    .build());
            throw new PaymentException("An error occurred while saving payment: " + ex.getMessage());
        }
    }

    private Account getAndSaveAccount(PaymentEvent paymentEvent) {
        Account account = accountRepository.findByAccountId(Long.valueOf(paymentEvent.getAccountId()))
                .orElseThrow(() -> {
                    String accountNotFoundMsg = "Account with ID:" + paymentEvent.getAccountId() + " can't be found";
                    storeErrorLogs(ErrorDto.builder()
                            .paymentId(paymentEvent.getPaymentId())
                            .errorType(ErrorType.DATABASE.name())
                            .errorDescription(accountNotFoundMsg)
                            .build());
                    return new AccountException(accountNotFoundMsg);
                });
        account.setLastPaymentDate(LocalDate.now());
        return saveAccount(paymentEvent.getPaymentId(), account);
    }

    private Account saveAccount(String paymentId, Account account) {
        try {
            return accountRepository.save(account);
        } catch (Exception ex) {
            storeErrorLogs(ErrorDto.builder()
                    .paymentId(paymentId)
                    .errorType(ErrorType.DATABASE.name())
                    .errorDescription(ex.getMessage())
                    .build());
            throw new AccountException("An error occurred while saving account: " + ex.getMessage());
        }
    }

    private Mono<String> checkPaymentValid(PaymentEvent paymentEvent) {
        final String url = apiProducerHost + PAYMENT_URL_SUFFIX;
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paymentEvent))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(VALID_PAYMENT);
                    } else {
                        return Mono.just(INVALID_PAYMENT);
                    }
                });
    }

    private void storeErrorLogs(ErrorDto errorDto) {
        final String url = apiProducerHost + LOG_URL_SUFFIX;
        webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorDto))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe();
    }
}
