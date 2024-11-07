package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.exception.TransactionException;
import com.wezaam.withdrawal.model.PaymentMethod;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class WithdrawalProcessingService {
    @Autowired
    private PaymentProviderService paymentProviderService;

    /**
     * @param amount
     * @param paymentMethod
     * @return
     * @throws TransactionException
     */
    public Long sendToProcessing(Double amount, PaymentMethod paymentMethod) throws TransactionException {
        log.debug("Sending to Processing...");

        try {
            // Call a payment provider
            final Long transactionId = paymentProviderService.processPayment(amount, paymentMethod);
            // In case a transaction can be process
            // It generates a transactionId and process the transaction async
            return transactionId;
        } catch (final Exception exc) {
            // Otherwise it throws TransactionException
            log.error(exc.getMessage(), exc);
            throw new TransactionException();
        }
    }
}
