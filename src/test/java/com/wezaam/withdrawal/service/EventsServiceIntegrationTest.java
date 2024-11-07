package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.model.Withdrawal;
import com.wezaam.withdrawal.model.WithdrawalStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.KafkaException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EventsServiceIntegrationTest {
    private static final Long WITHDRAWAL_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long TRANSACTION_ID = 1L;

    @Autowired
    private EventsService eventsService;

    private static Withdrawal withdrawal;

    @BeforeAll
    static void setupWithdrawal() {
        withdrawal = constructWithdrawal();
    }

    @Disabled("Disabled because this passes only if kafka is up")
    @Test
    public void sendWithdrawal_success() {
        // Given
        // ...

        // When
        eventsService.send(withdrawal);

        // Then
        // ...
    }

    @Disabled("Disabled because this passes only if kafka is down")
    @Test
    public void sendWithdrawal_fail() {
        // Given
        // ...

        // When
        final KafkaException exception = assertThrows(
                KafkaException.class,
                () -> eventsService.send(withdrawal),
                "Expecting Kafka exception caused by connection timeout."
        );

        // Then
        assertTrue(exception.getMessage().contains("TimeoutException: Topic withdrawals not present in metadata after 15000 ms."));
    }

    private static Withdrawal constructWithdrawal() {
        final Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(WITHDRAWAL_ID);
        withdrawal.setTransactionId(TRANSACTION_ID);
        withdrawal.setAmount(1.23);
        withdrawal.setCreatedAt(Instant.now());
        withdrawal.setUserId(USER_ID);
        withdrawal.setStatus(WithdrawalStatus.PENDING);

        return withdrawal;
    }
}
