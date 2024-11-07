package com.provider.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Random;

@Log4j2
@Service
public class PaymentService {
    private static final Random RANDOM = new Random();

    /**
     * @param amount
     * @param paymentMethod
     * @return
     * @throws Exception
     */
    public Long processPayment(Double amount, int paymentMethod) throws Exception {
        log.debug("Processing payment of GBP" + amount + " via " + paymentMethod);

        if (RANDOM.nextInt(100) % 5 == 0) {
            throw new Exception("Error while processing payment of GBP" + amount + " via " + paymentMethod);
        }

        return System.nanoTime();
    }
}
