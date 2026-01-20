package com.ecommerce.kafka;

import com.ecommerce.notification.kafka.NotificationKafkaListener;
import com.ecommerce.notification.model.OrderCreatedEvent;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationKafkaListener listener;

    @Test
    void testHandleOrderCreated() throws Exception {
        // Given
        String eventJson = "{\"orderId\":\"ORDER-123\",\"userId\":\"user-1\",\"userEmail\":\"test@example.com\",\"totalAmount\":99.99,\"timestamp\":1234567890}";

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("ORDER-123")
                .userId("user-1")
                .userEmail("test@example.com")
                .totalAmount(99.99)
                .timestamp(1234567890L)
                .build();

        when(objectMapper.readValue(eq(eventJson), eq(OrderCreatedEvent.class))).thenReturn(event);
        doNothing().when(notificationService).sendNotification(anyString(), anyString(), anyString(), anyMap());

        // When
        listener.handleOrderCreated(eventJson, "order.created", 0L);

        // Then
        verify(notificationService, times(1)).sendNotification(
                eq("order.created"),
                eq("user-1"),
                eq("test@example.com"),
                anyMap()
        );
    }
}
