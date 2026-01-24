package com.orderplatform.order.domain.enums;

public enum OrderStatus {
    NEW,
    RESERVED,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus nextStatus) {
        return switch(this) {
            case NEW -> nextStatus == RESERVED || nextStatus == CANCELLED;
            case RESERVED -> nextStatus == PAID || nextStatus == CANCELLED;
            case PAID -> nextStatus == SHIPPED || nextStatus == CANCELLED;
            case SHIPPED -> nextStatus == COMPLETED || nextStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
