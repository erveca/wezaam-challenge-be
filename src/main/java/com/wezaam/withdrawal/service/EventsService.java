package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.model.Withdrawal;
import com.wezaam.withdrawal.model.WithdrawalScheduled;
import com.wezaam.withdrawal.repository.WithdrawalRepository;
import com.wezaam.withdrawal.repository.WithdrawalScheduledRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Log4j2
@Service
public class EventsService {
    private static final String KAFKA_TOPIC = "withdrawals";

    @Autowired
    private KafkaTemplate<String, Withdrawal> kafkaTemplate;
/*
    @Autowired
    private WithdrawalRepository withdrawalRepository;
    @Autowired
    private WithdrawalScheduledRepository withdrawalScheduledRepository;
*/
    @Async
    public void send(Withdrawal withdrawal) {
        // build and send an event in message queue async
        sendWithdrawal(withdrawal);
    }

    @Async
    public void send(WithdrawalScheduled withdrawal) {
        // build and send an event in message queue async
        sendWithdrawal(withdrawal);
    }

    private void sendWithdrawal(Withdrawal withdrawal) {
        kafkaTemplate.send(KAFKA_TOPIC, withdrawal)
                .completable()
                .thenAcceptAsync(result -> {
                    final ProducerRecord<String, Withdrawal> record = result.getProducerRecord();
                    log.debug("Key: {}, Partition: {}, Topic: {}, Value: {}", record.key(), record.partition(), record.topic(), record.value());
                    //withdrawal.setNotified(true);
                    //saveWithdrawal(withdrawal);
                }).exceptionallyAsync(throwable -> {
                            log.error(throwable.getMessage(), throwable);
                            //withdrawal.setNotified(false);
                            //saveWithdrawal(withdrawal);
                            return Void.TYPE.cast(null);
                        }
                );
    }
/*
    private void saveWithdrawal(Withdrawal withdrawal) {
        if (withdrawal instanceof WithdrawalScheduled withdrawalScheduled) {
            withdrawalScheduledRepository.save(withdrawalScheduled);
        } else {
            withdrawalRepository.save(withdrawal);
        }
    }
 */
}
