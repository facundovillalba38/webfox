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
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class PaymentService {
    private final static String VALID_PAYMENT = "Payment is Valid";
    private final static String INVALID_PAYMENT = "Payment is Invalid";
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private WebClient.Builder webClientBuilder;

    public PaymentService(PaymentRepository paymentRepository, AccountRepository accountRepository, WebClient.Builder webClientBuilder) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.webClientBuilder = webClientBuilder;
    }

    public void processPayment(PaymentEvent payment) {
        System.out.println("Processing payment: " + payment);
        Mono<String> response = checkPaymentValid(payment);
        processValidPayment(response, payment);
    }

    private void processValidPayment(Mono<String> response, PaymentEvent paymentEvent){
        if(response.equals(VALID_PAYMENT)){
            // get Account using account id
            Account account = accountRepository.findByAccountId(Long.valueOf(paymentEvent.getAccountId()))
                    .orElseThrow(() -> {
                        String accountNotFoundMsg = "Account with ID:"+paymentEvent.getAccountId()+" can't be found";
                        storeErrorLogs(ErrorDto.builder().paymentId(paymentEvent.getPaymentId()).errorType(ErrorType.DATABASE).errorDescription(accountNotFoundMsg).build());
                        throw new AccountException(accountNotFoundMsg);
                    });
            // save payment into DB
            createAndSavePayment(paymentEvent, account);
        }
        if(response.equals(INVALID_PAYMENT)){
            storeErrorLogs(ErrorDto.builder().paymentId(paymentEvent.getPaymentId()).errorType(ErrorType.NETWORK).errorDescription(INVALID_PAYMENT).build());
        }
    }

    private void createAndSavePayment(PaymentEvent paymentEvent, Account account){
        try{
            Payment payment = new Payment();
            payment.setPaymentId(paymentEvent.getPaymentId());
            payment.setPaymentType(paymentEvent.getPaymentType());
            payment.setCreatedOn(LocalDate.now());
            payment.setCreditCard(payment.getCreditCard());
            payment.setAccount(account);
            paymentRepository.save(payment);
        }catch (DataAccessException ex){
            storeErrorLogs(ErrorDto.builder().paymentId(paymentEvent.getPaymentId()).errorType(ErrorType.DATABASE).errorDescription(ex.getMessage()).build());
            throw new PaymentException("An error occurred while saving payment: "+ ex.getMessage());
        }
    }

    private Mono<String> checkPaymentValid(PaymentEvent paymentEvent){
        // not sure what this endpoints return in order to check if valid or not. In postman it returns a 200 and that's it
        // we can assume that 200 is that the payment is valid, and any other error Http Status is that the payment is invalid.
        final String url = "http://localhost:9000/payment";
        return webClientBuilder.build().post()
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

    private Mono<Object> storeErrorLogs(ErrorDto errorDto){
        final String url = "http://localhost:9000/log";
        return webClientBuilder.build().post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorDto))
                .retrieve()
                .bodyToMono(Object.class);
    }
}
