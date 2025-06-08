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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service responsible for processing payments and validating them through external API.
 * Handles payment creation, account updates, and error logging.
 */
@Slf4j
@Service
public class PaymentService {
    
    // Constants
    private static final String VALID_PAYMENT = "Payment is Valid";
    private static final String INVALID_PAYMENT = "Payment is Invalid";
    private static final String PAYMENT_ENDPOINT = "/payment";
    private static final String LOG_ENDPOINT = "/log";
    
    // Dependencies
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final String apiProducerHost;
    
    // WebClient instance - initialized after construction
    private WebClient webClient;

    public PaymentService(PaymentRepository paymentRepository, 
                         AccountRepository accountRepository,
                         @Value("${api.producer.host}") String apiProducerHost) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.apiProducerHost = apiProducerHost;
    }

    @PostConstruct
    public void initializeWebClient() {
        this.webClient = WebClient.builder()
            .baseUrl(apiProducerHost)
            .build();
    }

    /**
     * Processes a payment event by validating it and storing it if valid.
     * 
     * @param paymentEvent the payment event to process
     */
    public void processPayment(PaymentEvent paymentEvent) {
        log.info("Processing payment: id={}, account={}, amount={}", 
                paymentEvent.getPaymentId(), paymentEvent.getAccountId(), paymentEvent.getAmount());
        
        try {
            String validationResponse = validatePayment(paymentEvent).block();
            handleValidationResponse(validationResponse, paymentEvent);
        } catch (Exception e) {
            log.error("Error processing payment {}: {}", paymentEvent.getPaymentId(), e.getMessage());
            logError(paymentEvent.getPaymentId(), ErrorType.NETWORK, 
                    "Error processing payment: " + e.getMessage());
            throw new PaymentException("Failed to process payment", e);
        }
    }

    /**
     * Handles the payment validation response and processes accordingly.
     * 
     * @param validationResponse the response from payment validation
     * @param paymentEvent the payment event being processed
     */
    private void handleValidationResponse(String validationResponse, PaymentEvent paymentEvent) {
        if (VALID_PAYMENT.equals(validationResponse)) {
            processValidPayment(paymentEvent);
        } else if (INVALID_PAYMENT.equals(validationResponse)) {
            log.warn("Payment validation failed for payment {}", paymentEvent.getPaymentId());
            logError(paymentEvent.getPaymentId(), ErrorType.NETWORK, INVALID_PAYMENT);
        } else {
            log.error("Unexpected validation response for payment {}: {}", 
                     paymentEvent.getPaymentId(), validationResponse);
            logError(paymentEvent.getPaymentId(), ErrorType.NETWORK, 
                    "Unexpected validation response: " + validationResponse);
        }
    }

    /**
     * Processes a valid payment by updating account and creating payment record.
     * 
     * @param paymentEvent the validated payment event
     */
    private void processValidPayment(PaymentEvent paymentEvent) {
        try {
            Account account = updateAccountWithPayment(paymentEvent);
            createPaymentRecord(paymentEvent, account);
            log.info("Successfully processed payment {}", paymentEvent.getPaymentId());
        } catch (Exception e) {
            log.error("Error processing valid payment {}: {}", paymentEvent.getPaymentId(), e.getMessage());
            throw e; // Re-throw to maintain existing exception handling
        }
    }

    /**
     * Creates and saves a new payment record.
     * 
     * @param paymentEvent the payment event data
     * @param account the associated account
     */
    private void createPaymentRecord(PaymentEvent paymentEvent, Account account) {
        try {
            Payment payment = buildPayment(paymentEvent, account);
            paymentRepository.save(payment);
            log.debug("Payment record created for payment {}", paymentEvent.getPaymentId());
        } catch (Exception ex) {
            String errorMsg = "Error saving payment: " + ex.getMessage();
            log.error("Failed to save payment {}: {}", paymentEvent.getPaymentId(), errorMsg);
            logError(paymentEvent.getPaymentId(), ErrorType.DATABASE, errorMsg);
            throw new PaymentException(errorMsg, ex);
        }
    }

    /**
     * Builds a Payment entity from the payment event.
     * 
     * @param paymentEvent the payment event
     * @param account the associated account
     * @return the built Payment entity
     */
    private Payment buildPayment(PaymentEvent paymentEvent, Account account) {
        Payment payment = new Payment();
        payment.setPaymentId(paymentEvent.getPaymentId());
        payment.setPaymentType(paymentEvent.getPaymentType());
        payment.setCreatedOn(LocalDate.now());
        payment.setCreditCard(paymentEvent.getCreditCard());
        payment.setAccount(account);
        payment.setAmount(BigDecimal.valueOf(paymentEvent.getAmount()));
        return payment;
    }

    /**
     * Retrieves and updates account with last payment date.
     * 
     * @param paymentEvent the payment event
     * @return the updated account
     */
    private Account updateAccountWithPayment(PaymentEvent paymentEvent) {
        Account account = findAccountById(paymentEvent.getAccountId(), paymentEvent.getPaymentId());
        account.setLastPaymentDate(LocalDate.now());
        return saveAccount(account, paymentEvent.getPaymentId());
    }

    /**
     * Finds an account by ID or throws exception if not found.
     * 
     * @param accountId the account ID to find
     * @param paymentId the payment ID for error logging
     * @return the found account
     * @throws AccountException if account not found
     */
    private Account findAccountById(String accountId, String paymentId) {
        return accountRepository.findByAccountId(Long.valueOf(accountId))
                .orElseThrow(() -> {
                    String errorMsg = "Account with ID: " + accountId + " not found";
                    log.error("Account lookup failed for payment {}: {}", paymentId, errorMsg);
                    logError(paymentId, ErrorType.DATABASE, errorMsg);
                    return new AccountException(errorMsg);
                });
    }

    /**
     * Saves an account and handles any exceptions.
     * 
     * @param account the account to save
     * @param paymentId the payment ID for error logging
     * @return the saved account
     */
    private Account saveAccount(Account account, String paymentId) {
        try {
            Account savedAccount = accountRepository.save(account);
            log.debug("Account updated for payment {}", paymentId);
            return savedAccount;
        } catch (Exception ex) {
            String errorMsg = "Error saving account: " + ex.getMessage();
            log.error("Failed to save account for payment {}: {}", paymentId, errorMsg);
            logError(paymentId, ErrorType.DATABASE, errorMsg);
            throw new AccountException(errorMsg, ex);
        }
    }

    /**
     * Validates a payment through external API call.
     * 
     * @param paymentEvent the payment to validate
     * @return Mono containing validation response
     */
    private Mono<String> validatePayment(PaymentEvent paymentEvent) {
        return webClient.post()
                .uri(PAYMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paymentEvent))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        log.debug("Payment validation successful for {}", paymentEvent.getPaymentId());
                        return Mono.just(VALID_PAYMENT);
                    } else {
                        log.warn("Payment validation failed for {} with status {}", 
                                paymentEvent.getPaymentId(), response.statusCode());
                        return Mono.just(INVALID_PAYMENT);
                    }
                })
                .onErrorReturn(WebClientException.class, INVALID_PAYMENT)
                .doOnError(error -> log.error("Error validating payment {}: {}", 
                          paymentEvent.getPaymentId(), error.getMessage()));
    }

    /**
     * Logs error information to external service.
     * 
     * @param paymentId the payment ID
     * @param errorType the type of error
     * @param description the error description
     */
    private void logError(String paymentId, ErrorType errorType, String description) {
        try {
            ErrorDto errorDto = ErrorDto.builder()
                    .paymentId(paymentId)
                    .errorType(errorType.name())
                    .errorDescription(description)
                    .build();

            webClient.post()
                    .uri(LOG_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(errorDto))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                        result -> log.debug("Error logged successfully for payment {}", paymentId),
                        error -> log.error("Failed to log error for payment {}: {}", paymentId, error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Exception while logging error for payment {}: {}", paymentId, e.getMessage());
        }
    }
}