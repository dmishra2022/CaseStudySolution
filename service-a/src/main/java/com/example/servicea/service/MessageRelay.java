package com.example.servicea.service;

import com.example.servicea.entity.NotificationOutbox;
import com.example.servicea.repository.NotificationOutboxRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.example.servicea.dto.OrderStatus.PENDING;
import static com.example.servicea.dto.OrderStatus.PROCESSED;

@Service
@Slf4j
public class MessageRelay {

    private final NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;


    @Value("${service-b.url}")
    private String serviceBUrl;

    @Autowired
    public MessageRelay(NotificationOutboxRepository notificationOutboxRepository, RestTemplate restTemplate, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("serviceB");
    }
    public void processOutbox() {
        List<NotificationOutbox> outboxEntries = notificationOutboxRepository.findUnprocessedNotifications(PENDING);
        for (NotificationOutbox entry : outboxEntries) {
            try {
                // create headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                // request body parameters
                Map<String, Object> map = new HashMap<>();
                map.put("orderId", entry.getOrderId());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);

                ResponseEntity<String> response = CircuitBreaker.decorateSupplier(circuitBreaker, () ->
                        restTemplate.postForEntity(
                                serviceBUrl + "/api/order-notification",entity, String.class
                        )
                ).get(); // Execute the call

                if (response.getStatusCode().is2xxSuccessful()) {
                    entry.setStatus(PROCESSED);
                    notificationOutboxRepository.save(entry);
                }
            } catch (Throwable e) { // Catch all exceptions, including CircuitBreakerOpenException
                if (e instanceof HttpServerErrorException || e instanceof ResourceAccessException) {
                    log.error("Service B is unavailable: " , e.getMessage());
                }
                else if (e instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                    log.error("Circuit Breaker is OPEN: " , e.getMessage());
                }
                else {
                    log.error("Error sending notification: " , e.getMessage());
                }
            }
        }
    }
}
