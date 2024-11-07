package com.wezaam.withdrawal.service;

import com.wezaam.withdrawal.exception.TransactionException;
import com.wezaam.withdrawal.model.PaymentMethod;
import com.wezaam.withdrawal.model.Withdrawal;
import com.wezaam.withdrawal.model.WithdrawalScheduled;
import com.wezaam.withdrawal.model.WithdrawalStatus;
import com.wezaam.withdrawal.repository.PaymentMethodRepository;
import com.wezaam.withdrawal.repository.WithdrawalRepository;
import com.wezaam.withdrawal.repository.WithdrawalScheduledRepository;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Log
@Service
public class WithdrawalService {
    @Autowired
    private WithdrawalRepository withdrawalRepository;
    @Autowired
    private WithdrawalScheduledRepository withdrawalScheduledRepository;
    @Autowired
    private WithdrawalProcessingService withdrawalProcessingService;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private EventsService eventsService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public void create(Withdrawal withdrawal) {
        Withdrawal pendingWithdrawal = withdrawalRepository.save(withdrawal);

        executorService.submit(() ->
                withdrawalRepository.findById(pendingWithdrawal.getId())
                        .ifPresent(savedWithdrawal ->
                                paymentMethodRepository.findById(savedWithdrawal.getPaymentMethodId())
                                        .ifPresent(paymentMethod ->
                                                processWithdrawal(withdrawal, paymentMethod, withdrawalRepository::save)
                                        )
                        )
        );
    }

    public void schedule(WithdrawalScheduled withdrawalScheduled) {
        withdrawalScheduledRepository.save(withdrawalScheduled);
    }

    @Scheduled(fixedDelay = 5000)
    public void runScheduledTasks() {
        log.info("Running scheduled task...");
        withdrawalScheduledRepository.findAllByExecuteAtBefore(Instant.now())
                .stream().filter(withdrawalScheduled -> withdrawalScheduled.getStatus().equals(WithdrawalStatus.PENDING))
                .forEach(this::processScheduled);
    }
/*
    // TODO Set proper CRON expression to run this once a day or every some hours
    @Scheduled(fixedDelay = 5000)
    public void runExecutionFailedTasks() {
        log.info("Running failed task...");
        // Fetch all withdrawals with status = INTERNAL_ERROR or FAILED and retry sending them to the Payment Provider
        withdrawalScheduledRepository.findAllByStatusIn(List.of(WithdrawalStatus.INTERNAL_ERROR, WithdrawalStatus.FAILED))
                .forEach(this::processScheduled);
        withdrawalRepository.findAllByStatusIn(List.of(WithdrawalStatus.INTERNAL_ERROR, WithdrawalStatus.FAILED))
                .forEach(withdrawal ->
                        paymentMethodRepository.findById(withdrawal.getPaymentMethodId())
                                .ifPresent(paymentMethod ->
                                        processWithdrawal(withdrawal, paymentMethod, withdrawalRepository::save)
                                ));
    }

    // TODO Set proper CRON expression to run this ever hour maybe?
    @Scheduled(fixedDelay = 5000)
    public void runEventFailedTasks() {
        log.info("Running failed task...");
        // Fetch all withdrawals with status = PROCESSING and notified = false and retry sending the events
        withdrawalScheduledRepository.findAllByStatusAndNotified(WithdrawalStatus.PROCESSING, false)
                .forEach(eventsService::send);
        withdrawalRepository.findAllByStatusAndNotified(WithdrawalStatus.PROCESSING, false)
                .forEach(eventsService::send);
    }
*/
    private void processScheduled(WithdrawalScheduled withdrawal) {
        log.info("Processing scheduled withdrawal: " + withdrawal.getId());
        paymentMethodRepository.findById(withdrawal.getPaymentMethodId())
                .ifPresent(paymentMethod -> {
                            processWithdrawal(withdrawal, paymentMethod, withdrawalScheduledRepository::save);
                        }
                );
    }

    private <W extends Withdrawal> void processWithdrawal(W withdrawal, PaymentMethod paymentMethod, Function<W, W> function) {
        try {
            var transactionId = withdrawalProcessingService.sendToProcessing(withdrawal.getAmount(), paymentMethod);
            withdrawal.setStatus(WithdrawalStatus.PROCESSING);
            withdrawal.setTransactionId(transactionId);
        } catch (Exception e) {
            var withdrawalStatus = WithdrawalStatus.INTERNAL_ERROR;
            if (e instanceof TransactionException) {
                withdrawalStatus = WithdrawalStatus.FAILED;
            }
            withdrawal.setStatus(withdrawalStatus);
        } finally {
            function.apply(withdrawal);
            eventsService.send(withdrawal);
        }
    }
}
