package com.wezaam.withdrawal.model;

import lombok.ToString;

import javax.persistence.Entity;
import java.time.Instant;

@ToString
@Entity(name = "scheduled_withdrawals")
public class WithdrawalScheduled extends Withdrawal {

    private Instant executeAt;

    public Instant getExecuteAt() {
        return executeAt;
    }

    public void setExecuteAt(Instant executeAt) {
        this.executeAt = executeAt;
    }
}
