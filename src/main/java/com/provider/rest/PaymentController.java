package com.provider.rest;

import com.provider.service.PaymentService;
import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@Api
@RestController
public class PaymentController {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity pay(HttpServletRequest request) {
        String paymentMethodId = request.getParameter("paymentMethodId");
        String amount = request.getParameter("amount");

        if (paymentMethodId == null || amount == null) {
            return new ResponseEntity("Required params are missing", HttpStatus.BAD_REQUEST);
        }

        try {
            Long transactionId = paymentService.processPayment(Double.parseDouble(amount), Integer.parseInt(paymentMethodId));
            return new ResponseEntity(transactionId, HttpStatus.OK);
        } catch (final Exception exc) {
            log.error(exc.getMessage(), exc);
            return new ResponseEntity(exc.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
