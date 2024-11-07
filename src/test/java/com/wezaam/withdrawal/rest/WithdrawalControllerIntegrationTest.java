package com.wezaam.withdrawal.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wezaam.withdrawal.model.PaymentMethod;
import com.wezaam.withdrawal.model.User;
import com.wezaam.withdrawal.model.Withdrawal;
import com.wezaam.withdrawal.model.WithdrawalStatus;
import com.wezaam.withdrawal.repository.PaymentMethodRepository;
import com.wezaam.withdrawal.repository.UserRepository;
import com.wezaam.withdrawal.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WithdrawalControllerIntegrationTest {
    private static final double AMOUNT = 1.23;
    private static final double MAX_AMOUNT = 100;
    private static final Long TRANSACTION_ID = 1L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private WithdrawalController withdrawalController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private PaymentMethod paymentMethod;

    @BeforeEach
    void setup() {
        withdrawalRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        userRepository.deleteAll();

        user = createUser();
        paymentMethod = createPaymentMethod(user);
    }

    private User createUser() {
        final User user = new User();
        user.setFirstName("Ernesto");
        user.setPaymentMethods(new ArrayList<>());
        user.setMaxWithdrawalAmount(MAX_AMOUNT);

        return userRepository.save(user);
    }

    private PaymentMethod createPaymentMethod(final User user) {
        final PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName("My favourite payment method");
        paymentMethod.setUser(user);

        return paymentMethodRepository.save(paymentMethod);
    }

    @Test
    @DisplayName("Fetching all withdrawals returns an empty list when there is none in the database")
    public void findAllWithdrawals_success_when_no_withdrawals() throws Exception {
        mockMvc.perform(get("/find-all-withdrawals"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    @DisplayName("Fetching all withdrawals returns a list with one element when there is one in the database")
    public void findAllWithdrawals_success_when_one_withdrawal() throws Exception {
        final Withdrawal withdrawal = createWithdrawal();
        // TODO Need to fix datetime format before doing comparison
        //final String withdrawalStr = objectMapper.setDateFormat(DateFormat.getDateInstance())
        //        .writer().writeValueAsString(List.of(withdrawal));

        final String startsStr = "[{\"id\":" + withdrawal.getId() + ",\"transactionId\":" + withdrawal.getTransactionId() + ",\"amount\":" + withdrawal.getAmount() + ",\"createdAt\":";
        final String endsStr = "\"userId\":" + withdrawal.getUserId() + ",\"paymentMethodId\":" + withdrawal.getPaymentMethodId() + ",\"status\":\"" + withdrawal.getStatus() + "\"}]";

        mockMvc.perform(get("/find-all-withdrawals"))
                .andDo(print())
                .andExpect(status().isOk())
//                .andExpect(content().json(withdrawalStr));
                .andExpect(content().string(startsWith(startsStr)))
                .andExpect(content().string(endsWith(endsStr)));
    }

    private Withdrawal createWithdrawal() {
        final Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(user.getId());
        withdrawal.setPaymentMethodId(paymentMethod.getId());
        withdrawal.setCreatedAt(Instant.now());
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setAmount(AMOUNT);
        withdrawal.setTransactionId(TRANSACTION_ID);

        return withdrawalRepository.save(withdrawal);
    }
}
