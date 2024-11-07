Some notes to have into consideration:
- I have only had one day to work on the solution. So I am not really proud of it, but here is what I could finish.
- I forked the main branch and I made some changes to the original code.
- For the events system I have used a local instance of Kafka running on port 9092. Hence, this is needed to run the project and send the events to the kafka topic.
- For database, I used H2 for simplicity and because it didn't need extra configuration apart from original ones.
- I have developed several unit tests and also some integration tests for several services and one controller.
- I have created an extra controller to simulate the response from the payment provider API. It shouldn't be on the same Spring Boot application, but it was the fastest thing to do. This will return a successful or failure response based on a random number. Probability is 80% success VS 20% failure.  
- I tested the endpoints using Postman, and I have added the collection to the Git repository.
- I have changed Java version to 17, so I could use Pattern Matching in one condition.
- I have implemented retry on PaymentProviderService, so it is retried up to 3 times in case of an exception occurring inside the processPayment method. If those 3 attempts fail, the method annotated with @Recover would be executed. In this implementation, it simply throws the exception. However, this retry should be revised as if the amount to be withdrawed is greater than the limit set for that specific user, all reattempts will fail. There is no point in retry in this scenario. This could be achieved by creating specific Exceptions and use them properly instead of using the generic Exception class, which is not recommended at all. Again, I used this because of lack of time and to allow me to focus on other parts of the code.    

As for the "bonus" task, I already started working on it, but I left the code commented because I would need
to test it. I would approach it using this solution:
1. As we are sending the events to the Kafka topic asynchronously and this can fail, I would add those withdrawals to a new database table, or marked them somehow to be re-processed. In the commented solution I have added a new "notified" column to both Withdrawal and WithdrawalScheduled entities / tables.
2. Using a different CRON job / scheduled task I would fetch those failed withdrawals and try sending the events again.

Alternatively, we could try to add these failed withdrawals to a different Kafka topic or source, where a different microservice could process them later.
A different Kafka topic would only work if the issue was not caused by unable to connect to Kafka (but for example, a problem with the Kafka topic being used).

So basically that's it. I hope I had more time to keep working on the solution.
With more time I would afford following tasks:
- Implementing the project or at least one service or controller in Kotlin.
- Keep testing the functionality.
- Finish implementing and testing the proposed solution for the "bonus" task.
