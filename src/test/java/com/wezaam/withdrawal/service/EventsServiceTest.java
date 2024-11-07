package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.model.Withdrawal;
import com.wezaam.withdrawal.model.WithdrawalStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventsServiceTest {
    private static final String KAFKA_TOPIC = "withdrawals";
    private static final Long WITHDRAWAL_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long TRANSACTION_ID = 1L;

    @Mock
    private KafkaTemplate<String, Withdrawal> kafkaTemplate;

    @InjectMocks
    private EventsService eventsService;

    private static Withdrawal withdrawal;

    @BeforeAll
    static void setupWithdrawal() {
        withdrawal = constructWithdrawal();
    }

    @Test
    @DisplayName("When sending an event for a given withdrawal it succeeds")
    public void sendWithdrawal_success() {
        // Given
        final ProducerRecord<String, Withdrawal> producerRecord = new ProducerRecord<>(KAFKA_TOPIC, withdrawal);
        final RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition(KAFKA_TOPIC, 1), 0, 0, Instant.now().toEpochMilli(), 0L, 0, 0);
        final SendResult sendResult = new SendResult<>(producerRecord, recordMetadata);

        when(kafkaTemplate.send(anyString(), any(Withdrawal.class))).thenReturn(
                new CompletableToListenableFutureAdapter<>(CompletableFuture.completedFuture(sendResult))
        );

        // When
        eventsService.send(withdrawal);

        // Then
        verify(kafkaTemplate).send(eq(KAFKA_TOPIC), eq(withdrawal));
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("When sending an event for a given withdrawal it fails if cannot communicate with Kafka")
    public void sendWithdrawal_fail() {
        // Given
        when(kafkaTemplate.send(anyString(), any(Withdrawal.class))).thenReturn(
                new CompletableToListenableFutureAdapter<>(CompletableFuture.failedFuture(new RuntimeException("Error while sending withdrawal to Kafka topic!")))
        );

        // When
        eventsService.send(withdrawal);

        // Then
        verify(kafkaTemplate).send(eq(KAFKA_TOPIC), eq(withdrawal));
        verifyNoMoreInteractions(kafkaTemplate);
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
