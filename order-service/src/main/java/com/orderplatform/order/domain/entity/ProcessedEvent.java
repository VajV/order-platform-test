package com.orderplatform.order.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_event_id", columnList = "event_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    private String eventId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
