package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.model.PaymentMethod;
import com.wezaam.withdrawal.model.User;
import com.wezaam.withdrawal.repository.PaymentMethodRepository;
import com.wezaam.withdrawal.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentProviderServiceIntegrationTest {
    private static final double AMOUNT = 1.23;
    private static final double MAX_AMOUNT = 100;
    private static final Long TRANSACTION_ID = 1L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentProviderService paymentProviderService;

    @MockBean
    private RestTemplate restTemplate;

    private User user;
    private PaymentMethod paymentMethod;

    @BeforeEach
    void setup() {
        user = createUser();
        paymentMethod = createPaymentMethod(user);

        Mockito.when(restTemplate.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Long.class)))
                .thenReturn(ResponseEntity.ok(TRANSACTION_ID));
    }

    private User createUser() {
        final User user = new User();
        user.setFirstName("Ernesto");
        user.setPaymentMethods(new ArrayList<>());
        user.setMaxWithdrawalAmount(MAX_AMOUNT);

        return userRepository.save(user);
    }

    private PaymentMethod createPaymentMethod(final User user) {
        final PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName("My favourite payment method");
        paymentMethod.setUser(user);

        return paymentMethodRepository.save(paymentMethod);
    }


    @Test
    @DisplayName("When processing a payment succeeds and returns transaction id if payment provider responds with OK status")
    public void processPayment_success() throws Exception {
        // Given
        Mockito.when(restTemplate.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Long.class)))
                .thenReturn(ResponseEntity.ok(TRANSACTION_ID));

        // When
        final Long transactionId = paymentProviderService.processPayment(AMOUNT, paymentMethod);

        // Then
        Assertions.assertNotNull(transactionId);
        Assertions.assertEquals(TRANSACTION_ID, transactionId);
    }

    @Test
    @DisplayName("When processing a payment succeeds when amount is equal to max withdrawal amount")
    public void processPayment_success_maxWithdrawalAmount() throws Exception {
        // Given
        Mockito.when(restTemplate.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Long.class)))
                .thenReturn(ResponseEntity.ok(TRANSACTION_ID));

        // When
        final Long transactionId = paymentProviderService.processPayment(MAX_AMOUNT, paymentMethod);

        // Then
        Assertions.assertNotNull(transactionId);
        Assertions.assertEquals(TRANSACTION_ID, transactionId);
    }


    @Test
    @DisplayName("When processing a payment fails because the max withdrawal amount is exceeded")
    public void processPayment_fail_maxWithdrawalAmountExceeded() throws Exception {
        // Given
        // ...

        // When
        final Exception exception = assertThrows(
                Exception.class,
                () -> paymentProviderService.processPayment(MAX_AMOUNT + 0.01, paymentMethod),
                "Expecting Exception caused by exceeding withdrawal amount limit."
        );

        // Then
        assertTrue(exception.getCause().getMessage().contains("Trying to withdraw an amount higher than the maximum!"));
    }


    @Test
    @DisplayName("When processing a payment fails and throws exception if payment provider does not respond with OK status")
    public void processPayment_fail() throws Exception {
        // Given
        Mockito.when(restTemplate.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpEntity.class), ArgumentMatchers.eq(Long.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // When
        final Exception exception = assertThrows(
                Exception.class,
                () -> paymentProviderService.processPayment(MAX_AMOUNT, paymentMethod),
                "Expecting Exception caused by unprocessed payment on Payment Provider side."
        );

        // Then
        assertTrue(exception.getCause().getMessage().contains("Error occurred while processing the payment!"));
    }
}
