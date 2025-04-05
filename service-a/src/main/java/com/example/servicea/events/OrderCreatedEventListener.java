package com.example.servicea.events;

import com.example.servicea.service.MessageRelay;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
class OrderCreatedEventListener {

    private final MessageRelay messageRelay;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        messageRelay.processOutbox();
    }
}
