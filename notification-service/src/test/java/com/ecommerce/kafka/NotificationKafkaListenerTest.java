package com.ecommerce.kafka;

import com.ecommerce.notification.kafka.NotificationKafkaListener;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

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

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", "ORDER-123");
        event.put("userId", "user-1");
        event.put("userEmail", "test@example.com");
        event.put("totalAmount", 99.99);

        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(event);
        doNothing().when(notificationService).sendNotification(anyString(), anyString(), anyString(), anyMap());

        // When
        listener.handleOrderCreated(eventJson, "order.created", 0L);

        // Then
        verify(notificationService, times(1)).sendNotification(
                eq("order.created"),
                anyString(),
                anyString(),
                anyMap()
        );
    }
}
