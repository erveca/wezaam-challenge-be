package com.wezaam.withdrawal.repository;

import com.wezaam.withdrawal.model.Withdrawal;
import com.wezaam.withdrawal.model.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findAllByStatusIn(List<WithdrawalStatus> statuses);

    //List<Withdrawal> findAllByStatusAndNotified(WithdrawalStatus status, boolean notified);
}
