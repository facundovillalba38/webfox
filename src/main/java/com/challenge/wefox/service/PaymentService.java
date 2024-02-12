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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PaymentService {
    private final static String VALID_PAYMENT = "Payment is Valid";
    private final static String INVALID_PAYMENT = "Payment is Invalid";
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private WebClient webClient;

    public PaymentService(PaymentRepository paymentRepository, AccountRepository accountRepository) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        webClient = WebClient.create("http://localhost:9000");
    }

    public void processPayment(PaymentEvent payment) {
        System.out.println("Processing payment: " + payment);
        Mono<String> response = checkPaymentValid(payment);
        processValidPayment(response.block(), payment);
    }

    private void processValidPayment(String response, PaymentEvent paymentEvent){
        if(response.equals(VALID_PAYMENT)){
            Account account = getAndSaveAccount(paymentEvent);
            createAndSavePayment(paymentEvent, account);
        }
        if(response.equals(INVALID_PAYMENT)){
            storeErrorLogs(ErrorDto.builder().paymentId(paymentEvent.getPaymentId()).errorType(ErrorType.NETWORK.name()).errorDescription(INVALID_PAYMENT).build());
        }
    }

    private void createAndSavePayment(PaymentEvent paymentEvent, Account account){
        try{
            Payment payment = new Payment();
            payment.setPaymentId(paymentEvent.getPaymentId());
            payment.setPaymentType(paymentEvent.getPaymentType());
            payment.setCreatedOn(LocalDate.now());
            payment.setCreditCard(paymentEvent.getCreditCard());
            payment.setAccount(account);
            payment.setAmount(BigDecimal.valueOf(paymentEvent.getAmount()));
            paymentRepository.save(payment);
        }catch (PaymentException ex){
            storeErrorLogs(ErrorDto.builder().paymentId(paymentEvent.getPaymentId()).errorType(ErrorType.DATABASE.name()).errorDescription(ex.getMessage()).build());
            throw new PaymentException("An error occurred while saving payment: "+ ex.getMessage());
        }
    }

    private Account getAndSaveAccount(PaymentEvent paymentEvent){
        Account account = accountRepository.findByAccountId(Long.valueOf(paymentEvent.getAccountId()))
                .orElseThrow(() -> {
                    String accountNotFoundMsg = "Account with ID:"+paymentEvent.getAccountId()+" can't be found";
                    storeErrorLogs(ErrorDto.builder().paymentId(paymentEvent.getPaymentId()).errorType(ErrorType.DATABASE.name()).errorDescription(accountNotFoundMsg).build());
                    throw new AccountException(accountNotFoundMsg);
                });
        account.setLastPaymentDate(LocalDate.now());
        return saveAccount(paymentEvent.getPaymentId(), account);
    }

    private Account saveAccount(String paymentId, Account account){
        try{
            return accountRepository.save(account);
        }catch (AccountException ex){
            storeErrorLogs(ErrorDto.builder().paymentId(paymentId).errorType(ErrorType.DATABASE.name()).errorDescription(ex.getMessage()).build());
            throw new AccountException("An error occurred while saving account: "+ ex.getMessage());
        }
    }

    private Mono<String> checkPaymentValid(PaymentEvent paymentEvent){
        final String url = "http://localhost:9000/payment";
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

    private void storeErrorLogs(ErrorDto errorDto){
        webClient.post()
                .uri("/log")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorDto))
                .retrieve()
                .bodyToMono(String.class);
    }
}
