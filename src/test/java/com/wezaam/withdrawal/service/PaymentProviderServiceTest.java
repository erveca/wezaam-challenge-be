package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.model.PaymentMethod;
import com.wezaam.withdrawal.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PaymentProviderServiceTest {
    private static final Long TRANSACTION_ID = 1L;
    private static final Long PAYMENT_METHOD_ID = 1L;
    private static final String PAYMENT_METHOD_NAME = "My Favourite Payment Method";
    private static final double AMOUNT = 1.23;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentProviderService paymentProviderService;

    private static PaymentMethod paymentMethod;

    @BeforeAll
    static void setupPaymentMethod() {
        paymentMethod = constructPaymentMethod();
    }

    @BeforeEach
    void setupRestTemplate() {
        /*
        ReflectionTestUtils.setField(restTemplate, "paymentProviderProtocol", "http");
        ReflectionTestUtils.setField(restTemplate, "paymentProviderHost", "localhost");
        ReflectionTestUtils.setField(restTemplate, "paymentProviderPort", "7070");
        ReflectionTestUtils.setField(restTemplate, "paymentProviderPayPath", "pay");
        */
    }

    @Test
    @DisplayName("When processing a payment succeeds and returns transaction id if payment provider responds with OK status")
    public void processPayment_success() throws Exception {
        // Given
        final ResponseEntity<Long> response = new ResponseEntity<>(TRANSACTION_ID, HttpStatus.OK);

        Mockito.when(restTemplate.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.eq(Long.class)))
                .thenReturn(response);

        // When
        final Long transactionId = paymentProviderService.processPayment(AMOUNT, paymentMethod);

        // Then
        Assertions.assertNotNull(transactionId);
        Assertions.assertEquals(TRANSACTION_ID, transactionId);

        assertPaymentProviderRequest();
    }

    @Test
    @DisplayName("When processing a payment fails and throws exception if payment provider does not respond with OK status")
    public void processPayment_fail() throws Exception {
        // Given
        final ResponseEntity<Long> response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        Mockito.when(restTemplate.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.eq(Long.class)))
                .thenReturn(response);

        // When
        final Exception exception = assertThrows(
                Exception.class,
                () -> paymentProviderService.processPayment(AMOUNT, paymentMethod),
                "Expecting Exception caused by unprocessed payment on Payment Provider side."
        );

        // Then
        assertTrue(exception.getMessage().contains("Error occurred while processing the payment!"));

        assertPaymentProviderRequest();
    }

    private void assertPaymentProviderRequest() {
        final var urlArgCaptor = ArgumentCaptor.forClass(String.class);
        final var requestArgCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        Mockito.verify(restTemplate).postForEntity(urlArgCaptor.capture(), requestArgCaptor.capture(), ArgumentMatchers.eq(Long.class));
        Mockito.verifyNoMoreInteractions(restTemplate);

        //Assertions.assertEquals("http://localhost:7070/pay", urlArgCaptor.getValue());
        Assertions.assertEquals("null://null:0/null", urlArgCaptor.getValue());

        final HttpEntity<Map<String, Object>> request = requestArgCaptor.getValue();
        final Map<String, Object> params = request.getBody();
        Assertions.assertNotNull(params);
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(List.of(AMOUNT), params.getOrDefault("amount", null));
        Assertions.assertEquals(List.of(PAYMENT_METHOD_ID), params.getOrDefault("paymentMethodId", null));
    }


    private static PaymentMethod constructPaymentMethod() {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getMaxWithdrawalAmount()).thenReturn(100.0);

        final PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(PAYMENT_METHOD_ID);
        paymentMethod.setName(PAYMENT_METHOD_NAME);
        paymentMethod.setUser(user);

        return paymentMethod;
    }
}
