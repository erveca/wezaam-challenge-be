package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.model.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PaymentProviderService {
    @Value("${payment-provider.protocol:http}")
    private String paymentProviderProtocol;

    @Value("${payment-provider.host:localhost}")
    private String paymentProviderHost;

    @Value("${payment-provider.port:7070}")
    private int paymentProviderPort;

    @Value("${payment-provider.pay:pay}")
    private String paymentProviderPayPath;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * @param amount
     * @param paymentMethod
     * @return
     * @throws Exception
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 150, multiplier = 2.0))
    public Long processPayment(Double amount, PaymentMethod paymentMethod) throws Exception {
        int retry = 1;
        if (RetrySynchronizationManager.getContext() != null) {
            retry = RetrySynchronizationManager.getContext().getRetryCount();
        }
        log.debug("#{} - Processing Payment of GBP {} via {}", retry, amount, paymentMethod.getName());

        // In case max Withdrawal amount is a limit from Wezaam, and not from the payment provider
        // we will check that limit here before sending the payment to the provider
        if (amount > paymentMethod.getUser().getMaxWithdrawalAmount()) {
            log.error("Trying to withdraw an amount higher than the maximum!");
            // Use a different exception and configure @Retryable to ignore this case, as all reattempts will fail
            throw new Exception("Trying to withdraw an amount higher than the maximum!");
        }

        final MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("amount", amount);
        params.add("paymentMethodId", paymentMethod.getId());
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params);

        final String paymentProviderPayUrl =
                "%s://%s:%d/%s".formatted(paymentProviderProtocol, paymentProviderHost, paymentProviderPort, paymentProviderPayPath);
        log.error("Payment Provider Pay URL: {}", paymentProviderPayUrl);
        final ResponseEntity<Long> response = restTemplate.postForEntity(paymentProviderPayUrl, request, Long.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Error occurred while processing the payment!");
            throw new Exception("Error occurred while processing the payment!");
        }
        return response.getBody();
    }

    @Recover
    public Long recover(Exception e, Double amount, PaymentMethod paymentMethod) throws Exception {
        log.warn("Recovering from processing Payment of GBP {} via {}...", amount, paymentMethod.getName());
        log.warn(e.getMessage(), e);

        // TODO Implement a sensible logic

        throw e;
    }
}
