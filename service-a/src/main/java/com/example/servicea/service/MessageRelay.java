package com.example.servicea.service;

import com.example.servicea.dto.OrderStatus;
import com.example.servicea.entity.NotificationOutbox;
import com.example.servicea.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageRelay {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final RestTemplate restTemplate;


    @Value("${service-b.url}")
    private String serviceBUrl;


    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Transactional
    public void processOutbox() {
        List<NotificationOutbox> outboxEntries = notificationOutboxRepository.findUnprocessedNotifications(OrderStatus.PENDING);
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

                // Send notification to Service B
                restTemplate.postForEntity(
                        serviceBUrl + "/api/order-notification",
                        entity,
                        String.class);


                entry.setStatus(OrderStatus.PROCESSED);
                notificationOutboxRepository.save(entry); // Marking as processed in the *same* transaction
            } catch (Exception e) {
                // Log the error, and the @Retryable will handle retries.
                // We can Consider a mechanism like dead-letter queue for persistent failures.
                log.error("Failed to send notification for order {}: {}", entry.getOrderId(), e.getMessage());
                throw e; // Re-throw to trigger retry
            }
        }
    }
}
