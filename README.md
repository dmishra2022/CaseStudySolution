# Case Study System with two RESTful-Webservices Solution


## Case Study Questions and Implementation Details

### Questions
#### (A) Transactional Boundaries in Service A
Following are the transactional boundaries in Service A are considered:
1. **Order Creation**: 
   1. The primary transactional boundary in Service A is to cover the persistence of the order data. This ensures that the order is either fully saved or not saved at all. We want atomicity here.The `createOrder()` method in `OrderService` is annotated with `@Transactional`. This ensures that the order creation and notification message persistence occur within a single transaction.
   2. In a relational database, this will be a single database transaction. In a NoSQL database, appropriate transaction support or eventual consistency mechanisms would be necessary.   
2. **Notification to Service B**: 
   1. Ideally the notification to Service B should be part of the same logical transaction. However, this is often impractical in distributed systems due to the "two-phase commit" (2PC) problem.
   2. Instead, we're using a "transactional outbox" pattern. int this order is saved, and a notification message (e.g., to a message queue) is also saved within the same database transaction
   3. A separate process (e.g., a message relay) then reads the notification messages and sends them to Service B.

#### (B) Threading Model in Spring Boot: 
   1. Spring Boot uses a thread-per-request model by default. Each incoming HTTP request is handled by a separate thread from a thread pool managed by the embedded Tomcat or Jetty server.
   2. Request-processing classes (controllers, services) is stateless which avoid thread-safety issues. 
   3. Shared resources (e.g., database connections) is managed by Spring's dependency injection and transaction management features.
   4. Asynchronous processing using @Async or CompletableFuture is used for non-blocking operations, improving throughput
   5. The message relay is running in a separate thread, or better, as a scheduled task, to avoid blocking the user response

#### Failure Scenario
1. **Network Communication Issues Between Service A and Service B**:
   1. ***Service B Temporarily Unreachable***:
       1. Service A sends a notification to Service B, but the network is down.
       2. Here Service A has implemented a retry logic with exponential backoff.
       3. Added circuit breaker pattern to prevent overwhelming Service B with retries during prolonged outages.
       4. Added adequate logging and monitoring to track failed notification attempts.
       
   2.  ***Connection to Service B Timing Out or Being Lost***:
       1. Here we have configured an appropriate timeouts in the RestTemplate used by Service A.
       2. Here we have Handled SocketTimeoutException and ConnectException gracefully and used the same retry and circuit breaker logic. 

2. **Service A Crashing While Processing a User Request**:
    1. ***Possible Inconsistencies***:
        1. Crash Before Order Persistence: No order is saved, and no notification is sent.
        2. Crash After Order Persistence, Before Notification Outbox Entry: The order is saved, but Service B is not notified.
        3. Crash After Notification Outbox Entry, Before Notification Sent: The order is saved, and the notification is in the outbox, but not yet sent.
        4. Crash After Notification Sent, Before Notification Outbox Entry is updated to processed: The order is saved, the notification is sent, but the notification will be sent again on restart.

    2.  ***Reconciliation on Service A Restart***:
        1. Transactional Outbox: The message relay process will retry sending notifications from the NotificationOutbox table. Idempotent operations in Service B are crucial.
        2. Database Recovery: Database transactions ensure that partially saved orders are rolled back.
        3. Monitoring and Alerts: Set up monitoring to alert on failures, so that manual intervention can be taken if necessary.
        4. Idempotency in Service B: Service B must be designed to handle duplicate notifications gracefully. This is essential for eventual consistency.



## Implementation Details
Here's how the provided implementation addresses the requirement mentioned in case study:

**1. How does Service A ensure that order creation and notification message persistence occur atomically?**

* **Implementation:** Service A uses Spring's `@Transactional` annotation in the `OrderService.createOrder()` method. This ensures that the `Order` entity is saved to the `order_repository` and the `NotificationOutbox` entity is saved to the `notificationOutboxRepository` within a single database transaction. If either operation fails, the entire transaction is rolled back, preventing data inconsistency.

**2. How does Service A handle the scenario where Service B is temporarily unavailable?**

* **Implementation:**
    * Service A persists the notification message in the `notification_outbox` table.
    * The `MessageRelay` service in Service A periodically polls this table for unprocessed messages.
    * If Service B is unavailable, the `RestTemplate` in `MessageRelay` will throw an exception (e.g., `HttpServerErrorException`).
    * The `MessageRelay` is configured to retry sending the message (up to 5 times in the provided code).
    * The `NotificationOutbox` entry is only marked as processed if the call to Service B is successful. If all retries fail, the message remains in the outbox for later processing.

**3. How does Service B ensure that it processes each order notification only once, even if it receives duplicate notifications?**

* **Implementation:**
    * Service B uses the `OrderNotification` entity and the `order_notification` table to track processed notifications.
    * The `OrderNotificationService.handleOrderNotification()` method first checks if an `OrderNotification` record exists for the given `orderId`.
    * If it exists and is marked as processed, Service B does nothing.
    * If it exists and is not processed, Service B processes it and updates the record.
    * If it doesn't exist, Service B creates a new `OrderNotification` record, processes the notification, and marks it as processed.
    * This logic, particularly the check for `isProcessed`, ensures that Service B's processing logic (the `processOrder()` method) is idempotent.

**4. What are the benefits of using the Transactional Outbox pattern in this scenario?**

* **Reliability:** Guarantees that order notifications are eventually processed, even if Service B is temporarily unavailable.
* **Data Consistency:** Avoids data inconsistency between the `orders` and `order_notifications` tables. An order is only considered fully created if its notification is successfully persisted in the outbox.
* **Decoupling:** Service A doesn't need to know the details of how Service B handles notifications. It simply persists the notification and lets the `MessageRelay` handle the delivery.
* **Idempotency:** Service B can safely retry processing notifications without causing duplicate side effects.

**5. How do you handle message ordering?**

* **Implementation:**
    * Service A adds the notifications to the outbox table in the order the orders are created.
    * Service A's `MessageRelay` polls the outbox table and sends the notifications to Service B in the order they appear in the table (typically ordered by the primary key, which is usually an auto-incrementing ID).
    * This ensures that Service B receives the notifications in the same order that the orders were created in Service A.
    * Service B is implemented to handle notifications in the order they are received, although the current implementation doesn't have any specific logic that depends on the order.

## Service A

### Functionality

Service A is responsible for:

* Creating orders and persisting them in the `orders` table.
* Persisting order notification messages in the `notification_outbox` table.
* Retrying sending order notifications to Service B.

### API Details

* **POST /order**
    * Creates a new order.
    * Request Body:

        ```json
        {
            "customerId": "string",
            "product": "string",
            "quantity": number,
            "price": number
        }
        ```

    * Response:
        * 200 OK: Returns the created order with its ID.
        * 400 Bad Request: If the request body is invalid.

### Configuration

* `application.yaml`:
    * `server.port`: The port on which Service A runs (e.g., 8081).
    * `spring.datasource`: Configuration for the H2 database (or one can choose other database). **Important:** Use a separate database from Service B.
    * `spring.jpa`: JPA and Hibernate settings. `ddl-auto: create-drop` is suitable for development but use `validate` in production.
    * `management.endpoints.web.exposure.include`: Includes `health` and `prometheus` endpoints for monitoring.

## Service B

### Functionality

Service B is responsible for:

* Receiving order notifications from Service A.
* Processing the order notification (in this example, it logs the order ID).
* Ensuring that each notification is processed only once.

### API Details

* **POST /api/order-notification**
    * Receives an order notification.
    * Request Body: The `orderId` (a number) as plain text.
    * Response:
        * 200 OK: Indicates that the notification was received and processed.

### Configuration

* `application.yaml`:
    * `server.port`: The port on which Service B runs (e.g., 8082). **Important:** Use a different port from Service A.
    * `spring.datasource`: Configuration for the H2 database. **Important:** Use a separate database from Service A.
    * `spring.jpa`: JPA and Hibernate settings. `ddl-auto: create-drop` is suitable for development but use `validate` in production.
    * `management.endpoints.web.exposure.include`: Includes `health` and `prometheus` endpoints for monitoring.

## Running the Application

1.  **Clone the repository:**

    ```
    git clone <your_repository_url>
    cd <your_repository_url>
    ```

2.  **Build the services:**

    ```
    cd service-a
    mvn clean install
    cd ../service-b
    mvn clean install
    ```

3.  **Run the services:**

    ```
    cd service-a/target
    java -jar service-a-0.0.1-SNAPSHOT.jar & # Run in the background
    cd ../../service-b/target
    java -jar service-b-0.0.1-SNAPSHOT.jar & # Run in the background
    ```

    * Ensure that the services are running on different ports as configured in their respective `application.yaml` files.

4.  **Send an order to Service A:**

    ```
    curl -X POST -H "Content-Type: application/json" -d '{"customerId":"Test Customer","product":"Test Product","quantity":1, "price":100}' http://localhost:8081/api/orders
    ```

5.  **Check Service B logs:**

    * You should see a message in the Service B console indicating that the order notification was received and processed.

## Important Considerations

* **Database:** This example uses H2 in-memory databases for simplicity. In a production environment, use a persistent database like PostgreSQL, MySQL, or similar. Make sure each service uses its own database.
* **Message Broker:** This implementation does not use a message broker like RabbitMQ or Kafka. A message broker would provide more robust message delivery guarantees and scalability in a real-world scenario. The Transactional Outbox pattern is often used *in conjunction* with a message broker.
* **Idempotency:** The `OrderNotificationService` in Service B is designed to be idempotent. This is crucial for handling potential duplicate notifications. The `processOrder()` method should also be implemented to be idempotent.
* **Error Handling:** The `MessageRelay` in Service A includes basic retry logic. More sophisticated error handling might be necessary in a production environment, such as:
    * Dead-letter queues for failed messages.
    * Circuit breakers to prevent cascading failures.
    * Alerting and monitoring for failed deliveries.
* **Scalability:** For high-traffic systems, consider:
    * Scaling Service A and Service B horizontally.
    * Using a more robust message queuing system.
    * Optimizing the `MessageRelay` to process messages in batches.
* **Monitoring:** The `spring-boot-starter-actuator` dependency and the inclusion of `health` and `prometheus` in the `application.yaml` files enable basic monitoring. You can use these endpoints to check the health of the services and collect metrics. Consider using a monitoring system like Prometheus and Grafana for more comprehensive monitoring.


## Testing via swagger
* **Swagger UI**: The application includes Swagger UI for easy testing of the APIs. You can access it at `http://localhost:8081/swagger-ui/` for Service A and `http://localhost:8082/swagger-ui/` for Service B.
